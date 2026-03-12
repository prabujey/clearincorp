package com.trivine.llc.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.trivine.llc.api.dto.response.InvoiceDto;
import com.trivine.llc.api.dto.response.InvoiceItemDto;
import com.trivine.llc.api.dto.llc.request.PaymentRequestDto;
import com.trivine.llc.api.dto.response.PagedResponse;
import com.trivine.llc.api.dto.response.PaymentResponseDto;
import com.trivine.llc.api.entity.Company;
import com.trivine.llc.api.entity.CompanyServices;
import com.trivine.llc.api.entity.LoginUser;
import com.trivine.llc.api.entity.Payment;
import com.trivine.llc.api.exception.ResourceNotFoundException;
import com.trivine.llc.api.repository.CompanyRepository;
import com.trivine.llc.api.repository.CompanyServiceRepository;
import com.trivine.llc.api.repository.LoginUserRepository;
import com.trivine.llc.api.repository.PaymentRepository;
import com.trivine.llc.api.repository.projection.InvoiceRow;
import com.trivine.llc.api.service.utility.SendEmailService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.trivine.llc.api.constants.ServiceConstants.*;

@Slf4j
@Service
public class PaymentService {

    private final ObjectMapper objectMapper;
    private final CompanyRepository companyRepository;
    private final PaymentRepository paymentRepository;
    private final CompanyServiceRepository companyServiceRepository;
    private final LoginUserRepository loginUserRepository;
    private final SendEmailService sendEmailService;


    public PaymentService(@Value("${stripe.secretKey}") String secretKey, ObjectMapper objectMapper, CompanyRepository companyRepository, PaymentRepository paymentRepository, CompanyServiceRepository companyServiceRepository, LoginUserRepository loginUserRepository, SendEmailService sendEmailService) {
        this.objectMapper = objectMapper;
        this.companyRepository = companyRepository;
        this.paymentRepository = paymentRepository;
        this.companyServiceRepository = companyServiceRepository;
        this.loginUserRepository = loginUserRepository;
        this.sendEmailService = sendEmailService;
        Stripe.apiKey = secretKey;  // Set Stripe API Key
    }

    public PaymentResponseDto createPaymentIntent(PaymentRequestDto paymentRequest) throws StripeException {
        // RECOMMENDED: use BigDecimal to avoid rounding errors
        long amountInCents = java.math.BigDecimal.valueOf(paymentRequest.getAmount())
                .movePointRight(2)            // dollars -> cents
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .longValueExact();

        if (amountInCents <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }

        log.info("Creating PaymentIntent for user");

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency("usd")
                .setReceiptEmail(paymentRequest.getEmail())
                .setDescription("LLC Formation Payment for " + paymentRequest.getName())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params);

        return PaymentResponseDto.builder()
                .status("SUCCESS")
                .message("Payment Intent created successfully")
                .clientSecret(paymentIntent.getClientSecret())
                .build();
    }


    @Transactional
    public void saveStripePayment(JsonNode jsonNode, Long companyId) throws JsonProcessingException {
        // Convert the JsonNode to a JSON String for storage/logging
        String jsonString = objectMapper.writeValueAsString(jsonNode);
        log.info("Received Stripe JSON: {}", jsonString);

        // Extract paymentIntent node
        JsonNode paymentIntentNode = jsonNode.get("paymentIntent");
        if (paymentIntentNode == null) {
            throw new IllegalStateException("Missing 'paymentIntent' in JSON");
        }

        String transactionId = paymentIntentNode.has("id") ? paymentIntentNode.get("id").asText() : "N/A";
        double paymentAmount = paymentIntentNode.has("amount") ? paymentIntentNode.get("amount").asDouble() / 100 : 0.0;
        String currency = paymentIntentNode.has("currency") ? paymentIntentNode.get("currency").asText() : "N/A";
        String status = paymentIntentNode.has("status") ? paymentIntentNode.get("status").asText() : "N/A";

        JsonNode paymentMethodsNode = paymentIntentNode.get("payment_method_types");
        String paymentMethod = (paymentMethodsNode != null && paymentMethodsNode.isArray() && !paymentMethodsNode.isEmpty())
                ? paymentMethodsNode.get(0).asText() : "unknown";

        long createdTimestamp = paymentIntentNode.has("created") ? paymentIntentNode.get("created").asLong() : 0L;
        LocalDateTime createdOn = Instant.ofEpochSecond(createdTimestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        // Fetch company
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));

        // Create Payment
        Payment payment = new Payment();
        payment.setTransactionId(transactionId);
        payment.setCompany(company);
        payment.setPaymentAmount(paymentAmount);
        payment.setCurrency(currency);
        payment.setStatus(status);
        payment.setPaymentDate(createdOn);
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentBlob(jsonString);
        payment.setIsActive(true);

        String invoiceId = String.format("IN%07d", paymentRepository.findNextPaymentId());
        payment.setInvoiceId(invoiceId);

        // Save payment
        Payment updatedPayment = paymentRepository.save(payment);

        // Link payment to unpaid services
        List<CompanyServices> unpaidServices = companyServiceRepository.findActiveUnPaidCompanyServices(companyId);
        if (unpaidServices.isEmpty()) {
            throw new IllegalStateException("Payment added but no unpaid services found");
        }

        for (CompanyServices service : unpaidServices) {
            service.setPayment(updatedPayment);
            companyServiceRepository.save(service);
        }

        log.info("Payment saved for companyId={}, paymentId={}", companyId, updatedPayment.getPaymentId());

        // Email confirmation
        String userName = Optional.ofNullable(company.getLoginUser())
                .map(LoginUser::getFirstName)
                .orElse("User");

        String formattedDate = payment.getPaymentDate()
                .format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a"));

        boolean emailSent = sendEmailService.sendPaymentConfirmationEmail(
                FROM_EMAIL,
                company.getLoginUser().getEmail(),
                PAYMENT_SUBJECT,
                userName,
                String.valueOf(updatedPayment.getPaymentAmount()),
                updatedPayment.getTransactionId(),
                formattedDate,
                company.getCompanyName()
        );
        if (!emailSent) {
            log.warn("Failed to send payment confirmation email for companyId={}", companyId);
        }
    }

//    public InvoiceDto getInvoice(Payment payment) {
//
//        List<CompanyServices> companyServicesList=companyServiceRepository.findByPayment_PaymentId(payment.getPaymentId());
//        Optional<Company> companyOptional=companyRepository.findByCompanyId(payment.getCompany().getCompanyId());
//        Company company=companyOptional.get();
//        LoginUser loginUser = company.getLoginUser();
//        String billToEmail = loginUser.getEmail();
//        InvoiceDto invoiceDto=new InvoiceDto();
//        invoiceDto.setId(payment.getPaymentId());
//        invoiceDto.setCompanyId(company.getCompanyId());
//        invoiceDto.setBillTo(company.getCompanyName()+" "+company.getSuffixMaster().getSuffix());
//        invoiceDto.setBillToAddress(company.getStreetAddress1());
//        invoiceDto.setBillToEmail(billToEmail);
//        invoiceDto.setBillToPhone(company.getCompanyPhone1());
//        invoiceDto.setState(company.getState());
//        invoiceDto.setBillFrom(BILL_FROM_NAME);
//        invoiceDto.setBillFromEmail(BILL_FROM_EMAIL);
//        invoiceDto.setBillFromAddress(BILL_FROM_ADDRESS);
//        invoiceDto.setBillFromPhone(BILL_FROM_PHONE);
//        invoiceDto.setOrderDate(payment.getPaymentDate());
//        invoiceDto.setStatus(payment.getStatus());
//        List<InvoiceItemDto> invoiceItemDtos = getInvoiceItemDtos(companyServicesList);
//        invoiceDto.setOrders(invoiceItemDtos);
//        invoiceDto.setGrandTotal(payment.getPaymentAmount());
//        invoiceDto.setInvoiceId(payment.getInvoiceId());
//        return invoiceDto;
//    }
//
//    private static @NotNull List<InvoiceItemDto> getInvoiceItemDtos(List<CompanyServices> companyServicesList) {
//        List<InvoiceItemDto> invoiceItemDtos=new ArrayList<>();
//        for(CompanyServices companyServices: companyServicesList){
//
//            InvoiceItemDto invoiceItemDto =new InvoiceItemDto();
//            if(companyServices.getServiceMaster().getServiceId()==4)
//                invoiceItemDto.setItemName("State Fee");
//            else
//                invoiceItemDto.setItemName(companyServices.getServiceMaster().getServiceName());
//            invoiceItemDto.setUnitPrice(companyServices.getServicePrice());
//            invoiceItemDto.setUnits(1L);
//            invoiceItemDto.setUnitTotalPrice(companyServices.getServicePrice());
//            invoiceItemDtos.add(invoiceItemDto);
//        }
//        return invoiceItemDtos;
//    }

//    public List<InvoiceDto> getUserInvoice(Long loginUserId) {
//        List<InvoiceDto> invoiceDtoList = new ArrayList<>();
//        List<Payment> paymentList = paymentRepository.findPaymentsByCompanyLoginUserId(loginUserId);
//        for (Payment payment : paymentList) {
//            InvoiceDto invoiceDto = getInvoice(payment);  // Convert Payment to InvoiceDto
//            invoiceDtoList.add(invoiceDto);
//        }
//        return invoiceDtoList;
//    }
//
//    public InvoiceDto getInvoiceById(Long paymentId) {
//        Payment payment = paymentRepository.findById(paymentId)
//                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));
//
//        return getInvoice(payment); // Convert Payment to InvoiceDto
//    }
//
//
//    public PagedResponse<InvoiceDto> getPaginatedInvoicesBySearch(int page, int size, String search) {
//        Pageable pageable = PageRequest.of(page, size);
//
//        Page<Payment> paymentPage = StringUtils.hasText(search)
//                ? paymentRepository.searchInvoices(search.toLowerCase(), pageable)
//                : paymentRepository.findAll(pageable);
//
//        List<InvoiceDto> invoiceDtoList = paymentPage.getContent().stream()
//                .map(this::getInvoice)
//                .toList();
//
//        return new PagedResponse<>(
//                invoiceDtoList,
//                paymentPage.getNumber(),
//                paymentPage.getSize(),
//                paymentPage.getTotalElements(),
//                paymentPage.getTotalPages(),
//                paymentPage.isLast()
//        );
//    }


    @Transactional(readOnly = true)
    public List<InvoiceDto> getUserInvoiceOptimized(Long loginUserId) {

        List<InvoiceRow> rows = paymentRepository.findInvoiceRows(loginUserId);

        Map<Long, InvoiceDto> map = new LinkedHashMap<>();

        for (InvoiceRow r : rows) {
            InvoiceDto inv = map.computeIfAbsent(r.getPaymentId(), id -> {
                InvoiceDto dto = new InvoiceDto();
                dto.setId(r.getPaymentId());
                dto.setCompanyId(r.getCompanyId());
                dto.setInvoiceId(r.getInvoiceId());
                dto.setStatus(r.getStatus());
                dto.setOrderDate(r.getPaymentDate());

                String suffix = (r.getSuffix() == null) ? "" : r.getSuffix();
                dto.setBillTo(r.getCompanyName() + (suffix.isBlank() ? "" : " " + suffix));

                dto.setBillToEmail(r.getBillToEmail());
                dto.setBillToAddress(r.getStreetAddress1());
                dto.setBillToPhone(r.getCompanyPhone1());
                dto.setState(r.getState());

                dto.setBillFrom(BILL_FROM_NAME);
                dto.setBillFromEmail(BILL_FROM_EMAIL);
                dto.setBillFromAddress(BILL_FROM_ADDRESS);
                dto.setBillFromPhone(BILL_FROM_PHONE);

                dto.setOrders(new ArrayList<>());
                dto.setGrandTotal(r.getPaymentAmount()); // keep SAME as your old logic
                return dto;
            });

            // orders list (1..5 rows)
            if (r.getServiceName() != null) {
                InvoiceItemDto item = new InvoiceItemDto();
                item.setItemName(r.getServiceName());
                item.setUnitPrice(BigDecimal.valueOf(r.getServicePrice() == null ? 0.0 : r.getServicePrice().doubleValue()));
                item.setUnits(1L); // your CompanyServices has no qty field
                item.setUnitTotalPrice(item.getUnitPrice() .multiply(BigDecimal.valueOf(item.getUnits())));
                inv.getOrders().add(item);
            }
        }

        return new ArrayList<>(map.values());
    }


    @Transactional(readOnly = true)
    public PagedResponse<InvoiceDto> getPaginatedInvoicesBySearchNew(int page, int size, String search) {

        Pageable pageable = PageRequest.of(page, size);

        Page<Long> idPage = org.springframework.util.StringUtils.hasText(search)
                ? paymentRepository.searchPaymentIds(search.toLowerCase(), pageable)
                : paymentRepository.findAllPaymentIds(pageable);

        List<Long> ids = idPage.getContent();

        List<InvoiceDto> invoices;
        if (ids.isEmpty()) {
            invoices = List.of();
        } else {
            List<InvoiceRow> rows = paymentRepository.findInvoiceRowsByPaymentIds(ids);
            Map<Long, InvoiceDto> map = groupToInvoices(rows);

            // preserve page order
            invoices = ids.stream()
                    .map(map::get)
                    .filter(Objects::nonNull)
                    .toList();
        }

        return new PagedResponse<>(
                invoices,
                idPage.getNumber(),
                idPage.getSize(),
                idPage.getTotalElements(),
                idPage.getTotalPages(),
                idPage.isLast()
        );
    }

    private Map<Long, InvoiceDto> groupToInvoices(List<InvoiceRow> rows) {
        Map<Long, InvoiceDto> map = new LinkedHashMap<>();

        for (InvoiceRow r : rows) {
            InvoiceDto inv = map.computeIfAbsent(r.getPaymentId(), id -> {
                InvoiceDto dto = new InvoiceDto();
                dto.setId(r.getPaymentId());
                dto.setCompanyId(r.getCompanyId());
                dto.setInvoiceId(r.getInvoiceId());
                dto.setStatus(r.getStatus());
                dto.setOrderDate(r.getPaymentDate());

                String suffix = (r.getSuffix() == null) ? "" : r.getSuffix();
                dto.setBillTo(r.getCompanyName() + (suffix.isBlank() ? "" : " " + suffix));

                dto.setBillToEmail(r.getBillToEmail());
                dto.setBillToAddress(r.getStreetAddress1());
                dto.setBillToPhone(r.getCompanyPhone1());
                dto.setState(r.getState());

                dto.setBillFrom(BILL_FROM_NAME);
                dto.setBillFromEmail(BILL_FROM_EMAIL);
                dto.setBillFromAddress(BILL_FROM_ADDRESS);
                dto.setBillFromPhone(BILL_FROM_PHONE);

                dto.setOrders(new ArrayList<>());
                dto.setGrandTotal(r.getPaymentAmount()); // keep same logic as old
                return dto;
            });

            // orders[] (1..5 lines supported)
            if (r.getServiceName() != null) {
                InvoiceItemDto item = new InvoiceItemDto();
                item.setItemName(r.getServiceName());
                double unitPrice = (r.getServicePrice() == null) ? 0.0 : r.getServicePrice().doubleValue();
                item.setUnitPrice(BigDecimal.valueOf(unitPrice));
                item.setUnits(1L);
                item.setUnitTotalPrice(BigDecimal.valueOf(unitPrice));
                inv.getOrders().add(item);
            }
        }
        return map;
    }

    @Transactional(readOnly = true)
    public InvoiceDto getInvoiceByIdNew(Long paymentId) {

        List<InvoiceRow> rows = paymentRepository.findInvoiceRowsByPaymentId(paymentId);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Payment not found with ID: " + paymentId);
        }

        return groupToInvoices(rows).get(paymentId);
    }



}

