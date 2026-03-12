package com.trivine.llc.api.service;

import com.trivine.llc.api.constants.ServiceConstants;
import com.trivine.llc.api.dto.*;
import com.trivine.llc.api.dto.response.*;
import com.trivine.llc.api.dto.llc.request.*;
import com.trivine.llc.api.entity.*;
import com.trivine.llc.api.exception.ResourceNotFoundException;
import com.trivine.llc.api.mapper.CompanyLiteMapper;
import com.trivine.llc.api.mapper.CompanyMapper;
import com.trivine.llc.api.repository.*;
import com.trivine.llc.api.repository.projection.CompanySlim;
import com.trivine.llc.api.repository.projection.StatusCountProjection;
import com.trivine.llc.api.service.utility.SendEmailService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.cache.annotation.Cacheable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyService {
    private final CompanyMapper companyMapper;
    private final CompanyRepository companyRepository;
    private final FormationStatusRepository repo;
    private final ServiceMasterRepository serviceMasterRepository;
    private final CompanyServiceRepository companyServiceRepository;
    private final StateFeeRepository stateFeeRepository;
    private final CompanyPrincipalRepository companyPrincipalRepository;
    private final ManagerMemberService managerMemberService;
    private final MemberService memberService;
    private final SuffixMasterRepository suffixMasterRepository;
    private final FormationStatusRepository formationStatusRepository;
    private final FormationStatusMasterRepository formationStatusMasterRepository;
    private final CompanyFillingAuditService companyFillingAuditService;
    private final RegisteredAgentRepository registeredAgentRepository;
    private final ZipLookupService zipLookupService;
    private final CompanyLiteMapper mapper;
    private final EinDetailsService einDetailsService;
    @PersistenceContext
    private final EntityManager entityManager;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final DocumentMasterRepository documentMasterRepository;
    private final SendEmailService sendEmailService;

    /**
     * Default value placeholder used by Swagger - should be filtered out
     */
    private static final String SWAGGER_DEFAULT = "string";

    @Transactional(readOnly = true)
    @Cacheable("states")
    public List<String> getStateKeys() {
        return stateFeeRepository.findAllActiveStateNames();
    }

    @Transactional(readOnly = true)
    public List<String> getAllSuffix() {
        log.info("Fetching all suffix values");
        return suffixMasterRepository.findAll().stream()
                .map(SuffixMaster::getSuffix)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable("suffixes")
    public List<String> getActiveSuffixes() {
        return suffixMasterRepository.findAllActiveSuffixes();
    }

    @Transactional
    public CompanyResponseDto saveCompanyState(CompanyStateDto companyStateDto) {
        log.info("Saving or updating company state for user ID: {}", companyStateDto.getLoginUserId());

        LoginUser loginUser = new LoginUser();
        loginUser.setLoginUserId(companyStateDto.getLoginUserId());

        Company company;

        if (companyStateDto.getCompanyId() != null) {
            company = companyRepository.findById(companyStateDto.getCompanyId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Company not found with ID: " + companyStateDto.getCompanyId()));
        } else {
            company = new Company();
        }

        company.setState(companyStateDto.getState());
        company.setLoginUser(loginUser);
        company.setCreatedOn(LocalDateTime.now());
        company.setLastUpdated(LocalDateTime.now());

        Company savedCompany = companyRepository.save(company);

        FormationStatusMaster initialStatus = formationStatusMasterRepository
                .findByFormationStatusName("initial")
                .orElseThrow(() -> new ResourceNotFoundException("Initial status not found in FormationStatusMaster"));

        transitionFormationStatus(savedCompany, savedCompany.getLoginUser(), initialStatus);

        CompanyResponseDto dto1 = addCreationService(savedCompany.getCompanyId());

        if (!"Creation Service Added Successfully".equals(dto1.getMessage())) {
            throw new RuntimeException("State service adding failed");
        }

        String message = (companyStateDto.getCompanyId() != null) ? "State Updated Successfully"
                : "State Saved Successfully";
        return new CompanyResponseDto(message, savedCompany.getCompanyId());
    }

    @Transactional
    public CompanyResponseDto saveCompanyName(CompanyRequestDto dto, Long companyId) {
        log.info("Saving company name for company ID: {}", companyId);

        SuffixMaster suffixMaster = suffixMasterRepository.findBySuffix(dto.getLlcSuffix()).orElseGet(() -> {
            SuffixMaster newSuffix = new SuffixMaster();
            newSuffix.setSuffix(dto.getLlcSuffix());
            return suffixMasterRepository.save(newSuffix);
        });

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));

        company.setCompanyName(dto.getCompanyName());
        company.setSuffixMaster(suffixMaster);
        company.setLastUpdated(LocalDateTime.now());

        Company updatedCompany = companyRepository.save(company);

        if (updatedCompany.getCompanyName() == null) {
            return new CompanyResponseDto("Name Not Updated", null);
        }

        return new CompanyResponseDto("Company Name Saved Successfully", updatedCompany.getCompanyId());
    }

    @Transactional
    public CompanyResponseDto saveFormationDate(Long companyId, LocalDate formationDate) {
        log.info("Saving formation date for company ID: {}", companyId);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));

        company.setCompanyEffectiveDate(formationDate);
        company.setLastUpdated(LocalDateTime.now());
        companyRepository.save(company);

        return new CompanyResponseDto("Company formation date saved successfully.", company.getCompanyId());
    }

    @Transactional
    public CompanyResponseDto saveBusinessDescription(BusinessDetailsDto businessDetailsDto) {
        Long companyId = businessDetailsDto.getCompanyId();
        log.info("Saving business description for company ID: {}", companyId);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));

        company.setCompanyDesc(businessDetailsDto.getBusinessDescription());
        company.setLastUpdated(LocalDateTime.now());
        company.setTradeName(businessDetailsDto.getTradeName());
        company.setPrincipalActivity(businessDetailsDto.getPrincipalActivity());
        companyRepository.save(company);

        return new CompanyResponseDto("Company Details saved successfully.", company.getCompanyId());
    }

    @Transactional
    public CompanyResponseDto saveContactDetails(CompanyPrimaryContactDto companyPrimaryContactDto, Long companyId) {
        log.info("Saving contact details for company ID: {}", companyId);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));

        company.setCountry("USA");
        company.setCompanyEmail1(companyPrimaryContactDto.getEmail());
        company.setCompanyPhone1(companyPrimaryContactDto.getPhoneNumber());
        company.setLastUpdated(LocalDateTime.now());
        companyRepository.save(company);

        CompanyPrincipal principal = companyPrincipalRepository.findByCompanyId(companyId).orElseGet(() -> {
            CompanyPrincipal newPrincipal = new CompanyPrincipal();
            newPrincipal.setCompany(company);
            return newPrincipal;
        });

        principal.setFirstName(companyPrimaryContactDto.getFirstName());
        principal.setLastName(companyPrimaryContactDto.getLastName());
        principal.setEmail(companyPrimaryContactDto.getEmail());
        principal.setPhoneNumber(companyPrimaryContactDto.getPhoneNumber());
        principal.setCreatedOn(LocalDateTime.now());
        principal.setIsActive(true);

        companyPrincipalRepository.save(principal);

        return new CompanyResponseDto("Company contact details updated successfully.", companyId);
    }

    @Transactional
    public CompanyResponseDto saveMailingAttributes(CompanyMailingAttributesDto companyMailingAttributesDto,
            Long companyId) {
        log.info("Saving mailing attributes for company ID: {}", companyId);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));
        company.setStreetAddress1(companyMailingAttributesDto.getStreetAddress1());
        company.setStreetAddress2(companyMailingAttributesDto.getStreetAddress2());
        company.setCity(companyMailingAttributesDto.getCity());
        company.setZipCode(companyMailingAttributesDto.getZipCode());
        company.setLastUpdated(LocalDateTime.now());
        company.setUseAddress(companyMailingAttributesDto.getUseAddress());

        companyRepository.save(company);

        return new CompanyResponseDto("Company Mailing Address Saved Successfully", company.getCompanyId());
    }

    @Transactional
    public CompanyResponseDto updateRegForm1(Long companyId, Boolean regForm1) {
        log.info("Updating reg_form1 for company ID: {}", companyId);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));

        company.setRegForm1(regForm1);
        company.setLastUpdated(LocalDateTime.now());
        companyRepository.save(company);

        return new CompanyResponseDto("reg_form1 updated successfully", companyId);
    }

    @Transactional
    public CompanyResponseDto updateRegForm2(Long companyId, Boolean regForm2) {
        log.info("Updating reg_form2 for company ID: {}", companyId);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));

        company.setRegForm2(regForm2);
        company.setLastUpdated(LocalDateTime.now());
        companyRepository.save(company);

        if (Boolean.TRUE.equals(regForm2)) {
            FormationStatusMaster paidStatus = formationStatusMasterRepository.findByFormationStatusName("Paid")
                    .orElseThrow(() -> new ResourceNotFoundException("Formation status 'Paid' not found"));
            transitionFormationStatus(company, company.getLoginUser(), paidStatus);
        }

        return new CompanyResponseDto("reg_form2 updated successfully", companyId);
    }

    @Transactional
    public CompanyResponseDto updateRegForm3(Long companyId, Boolean regForm3, Boolean einChosen) {
        log.info("Updating reg_form3 for company ID: {}", companyId);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));

        company.setRegForm3(regForm3);
        company.setLastUpdated(LocalDateTime.now());
        companyRepository.save(company);

        if (Boolean.FALSE.equals(einChosen) && Boolean.TRUE.equals(regForm3)) {
            FormationStatusMaster savedStatus = formationStatusMasterRepository.findByFormationStatusName("saved")
                    .orElseThrow(() -> new ResourceNotFoundException("Formation status 'saved' not found"));
            transitionFormationStatus(company, company.getLoginUser(), savedStatus);

            try {
                companyFillingAuditService.setJsonDetails(companyId, company.getLoginUser().getLoginUserId(),
                        "User Added Data");
            } catch (Exception e) {
                log.error("Failed to set JSON details for company ID {}: {}", companyId, e.getMessage(), e);
            }
            sendFormationInProgressEmail(company);
        }

        return new CompanyResponseDto("reg_form3 updated successfully", companyId);
    }

    @Transactional
    public EinDetailsDto saveEin(EinDetailsDto einDetailsDto) throws Exception {
        log.info("saving ein details for company ID: {}", einDetailsDto.getCompanyId());
        return einDetailsService.saveEinDetails(einDetailsDto);
    }

    @Transactional
    public CompanyResponseDto updateRegForm4(Long companyId, Boolean regForm4) {
        log.info("Updating reg_form4 for company ID: {}", companyId);
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));

        company.setRegForm4(regForm4);
        company.setLastUpdated(LocalDateTime.now());
        companyRepository.save(company);

        if (Boolean.TRUE.equals(regForm4)) {
            FormationStatusMaster savedStatus = formationStatusMasterRepository.findByFormationStatusName("saved")
                    .orElseThrow(() -> new ResourceNotFoundException("Formation status 'saved' not found"));
            transitionFormationStatus(company, company.getLoginUser(), savedStatus);

            try {
                companyFillingAuditService.setJsonDetails(companyId, company.getLoginUser().getLoginUserId(),
                        "User Added Data");
            } catch (Exception e) {
                log.error("Failed to set JSON details for company ID {}: {}", companyId, e.getMessage(), e);
            }
            sendFormationInProgressEmail(company);
        }

        return new CompanyResponseDto("reg_form4 updated successfully", companyId);
    }

    @Transactional
    public String updateCompany(Long companyId, CompanyDetailsDto dto, String notes, Long loginUserId) {
        log.info("Updating company details for ID: {}", companyId);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));

        if (Boolean.FALSE.equals(company.getRegForm3())) {
            throw new IllegalStateException("reg_form3 is not completed for company ID: " + companyId);
        }

        if (isValidValue(dto.getState())) {
            company.setState(dto.getState());
        }

        if (dto.getFormationDate() != null) {
            company.setCompanyEffectiveDate(dto.getFormationDate());
        }

        if (dto.getCompanyRequestDto() != null) {
            CompanyRequestDto req = dto.getCompanyRequestDto();
            if (isValidValue(req.getCompanyName())) {
                company.setCompanyName(req.getCompanyName());
            }
            if (isValidValue(req.getLlcSuffix())) {
                suffixMasterRepository.findBySuffix(req.getLlcSuffix()).ifPresent(company::setSuffixMaster);
            }
        }

        if (dto.getCompanyMailingAttributesDto() != null) {
            CompanyMailingAttributesDto mailing = dto.getCompanyMailingAttributesDto();
            if (isValidValue(mailing.getStreetAddress1())) {
                company.setStreetAddress1(mailing.getStreetAddress1());
            }
            if (isValidValue(mailing.getStreetAddress2())) {
                company.setStreetAddress2(mailing.getStreetAddress2());
            }
            if (isValidValue(mailing.getCity())) {
                company.setCity(mailing.getCity());
            }
            if (isValidValue(mailing.getZipCode())) {
                company.setZipCode(mailing.getZipCode());
            }
        }

        if (dto.getCompanyPrimaryContactDto() != null) {
            savePrimaryContact(dto.getCompanyPrimaryContactDto(), companyId);
        }

        if (dto.getRegisteredAgentDto() != null) {
            updateRegisteredAgent(company, dto.getRegisteredAgentDto());
        }

        if (dto.getManagerMemberDtoList() != null && !dto.getManagerMemberDtoList().isEmpty()) {
            managerMemberService.saveAllManagers(dto.getManagerMemberDtoList(), companyId);
        }

        if (dto.getMemberMemberDtoList() != null && !dto.getMemberMemberDtoList().isEmpty()) {
            memberService.saveAllMembers(dto.getMemberMemberDtoList(), companyId);
        }

        companyRepository.save(company);

        entityManager.flush();
        applicationEventPublisher.publishEvent(new CompanyAuditEvent(companyId, loginUserId, notes));

        return "Company updated successfully";
    }

    @Transactional(readOnly = true)
    @Cacheable("documentTypes")
    public List<DocumentMasterDto> getDocumentTypeList() {
        return documentMasterRepository.findAllAsDto();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCompanyStatusPaged(int page, int size, String status, String state,
            String companyName, Boolean isExpedited) {
        Pageable pageable = PageRequest.of(page, size);
        FormationStatusPagedResponse serviceResponse = getRelevantStatusesPaged(pageable, status, state, companyName,
                isExpedited);
        Page<FormationStatus> pageResult = serviceResponse.getPage();

        List<StatusResponseDto> dtos = pageResult.getContent().stream().map(statusRecord -> {
            StatusResponseDto dto = new StatusResponseDto();
            dto.setCompanyId(statusRecord.getCompany().getCompanyId());
            dto.setIsExpeditedServiceSelected(statusRecord.getCompany().getIsExpeditedServiceSelected());
            String suffix = Optional.ofNullable(statusRecord.getCompany().getSuffixMaster())
                    .map(SuffixMaster::getSuffix).orElse("");

            dto.setCompanyName(statusRecord.getCompany().getCompanyName() + (suffix.isEmpty() ? "" : " " + suffix));
            dto.setStatus(statusRecord.getStatus().getFormationStatusName());
            dto.setDate(statusRecord.getCompany().getLastUpdated().toLocalDate());
            dto.setState(statusRecord.getCompany().getState());
            dto.setIsEinSelected(statusRecord.getCompany().getRegForm4());

            return dto;
        }).collect(Collectors.toList());

        Map<String, Object> finalResponse = new HashMap<>();
        finalResponse.put("content", dtos);
        finalResponse.put("statusCounts", serviceResponse.getStatusCounts());
        finalResponse.put("pageNumber", pageResult.getNumber());
        finalResponse.put("pageSize", pageResult.getSize());
        finalResponse.put("totalElements", pageResult.getTotalElements());
        finalResponse.put("totalPages", pageResult.getTotalPages());
        finalResponse.put("last", pageResult.isLast());

        return finalResponse;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getEinStatusPaged(int page, int size, String state, String companyName,
            Boolean isExpedited) {
        Pageable pageable = PageRequest.of(page, size);

        Page<CompanyServices> pageResult = companyServiceRepository
                .findAllUncompletedPaidEinWhereCompanyRegForm4TrueFiltered(
                        state, companyName, isExpedited, pageable);

        List<EinResponseDto> content = pageResult.getContent().stream().map(cs -> {
            Company c = cs.getCompany();

            EinResponseDto dto = new EinResponseDto();
            dto.setCompanyId(c.getCompanyId());
            dto.setIsExpeditedServiceSelected(c.getIsExpeditedServiceSelected());

            String suffix = Optional.ofNullable(c.getSuffixMaster())
                    .map(SuffixMaster::getSuffix)
                    .orElse("");
            dto.setCompanyName(c.getCompanyName() + (suffix.isEmpty() ? "" : " " + suffix));

            if (cs.getServiceCreatedDate() != null) {
                dto.setDate(cs.getServiceCreatedDate().toLocalDate());
            }
            dto.setState(c.getState());
            return dto;
        }).toList();

        Map<String, Object> resp = new HashMap<>();
        resp.put("content", content);
        resp.put("pageNumber", pageResult.getNumber());
        resp.put("pageSize", pageResult.getSize());
        resp.put("totalElements", pageResult.getTotalElements());
        resp.put("totalPages", pageResult.getTotalPages());
        resp.put("last", pageResult.isLast());
        return resp;
    }

    @Transactional
    public String savePrimaryContact(CompanyPrimaryContactDto dto, Long companyId) {
        log.info("Saving primary contact for company ID: {}", companyId);

        CompanyPrincipal principal = companyPrincipalRepository
                .findByCompanyId(companyId)
                .orElse(null);

        if (principal == null) {
            principal = new CompanyPrincipal();
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));
            principal.setCompany(company);
            principal.setCreatedOn(LocalDateTime.now());
            principal.setIsActive(true);
        }

        if (dto.getFirstName() != null)
            principal.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null)
            principal.setLastName(dto.getLastName());
        if (dto.getEmail() != null)
            principal.setEmail(dto.getEmail());
        if (dto.getPhoneNumber() != null)
            principal.setPhoneNumber(dto.getPhoneNumber());

        companyPrincipalRepository.save(principal);
        log.info("Primary contact saved for company ID: {}", companyId);

        return "Primary contact saved successfully";
    }

    /**
     * @deprecated Use {@link #savePrimaryContact(CompanyPrimaryContactDto, Long)}
     *             instead
     */
    @Deprecated
    public String save(CompanyPrimaryContactDto dto, Long companyId) {
        return savePrimaryContact(dto, companyId);
    }

    @Transactional(readOnly = true)
    public CompanyDto findByCompanyId(Long companyId) {
        log.info("Fetching company with ID: {}", companyId);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));
        return companyMapper.toDto(company);
    }

    @Transactional
    public CompanyResponseDto addEinService(Long companyId, Boolean einRequired) {
        log.info("Adding EIN Service for company ID: {}", companyId);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company Not Found - ID: " + companyId));

        ServiceMaster serviceMaster = serviceMasterRepository.findByServiceName("EIN Registration")
                .orElseThrow(() -> new ResourceNotFoundException("Service Not Found - EIN Registration"));

        CompanyServices companyServices = companyServiceRepository
                .findByCompanyAndServiceMaster(company, serviceMaster)
                .orElse(new CompanyServices());

        companyServices.setCompany(company);
        companyServices.setServiceMaster(serviceMaster);
        companyServices.setTrueFalse(einRequired);
        companyServices.setServicePrice(serviceMaster.getServicePrice());
        companyServices.setStatus("wait");

        companyServiceRepository.save(companyServices);

        return new CompanyResponseDto("EIN Service Added Successfully", companyId);
    }

    @Transactional
    public CompanyResponseDto addOperatingAggrementService(Long companyId, Boolean required) {
        log.info("Adding Operating Agreement Service for company ID: {}", companyId);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));

        ServiceMaster serviceMaster = serviceMasterRepository.findByServiceName("Operating Agreement")
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: Operating Agreement"));

        CompanyServices companyServices = companyServiceRepository
                .findByCompanyAndServiceMaster(company, serviceMaster)
                .orElse(new CompanyServices());

        companyServices.setCompany(company);
        companyServices.setServiceMaster(serviceMaster);
        companyServices.setTrueFalse(required);
        companyServices.setServiceCreatedDate(LocalDateTime.now());
        companyServices.setServicePrice(serviceMaster.getServicePrice());
        companyServices.setStatus("wait");

        companyServiceRepository.save(companyServices);

        return new CompanyResponseDto("Operating Agreement Service Added Successfully", companyId);
    }

    @Transactional
    public CompanyResponseDto addExpeditService(Long companyId, Boolean required) {
        log.info("Adding Expedited Service for company ID: {}", companyId);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));

        ServiceMaster serviceMaster = serviceMasterRepository.findByServiceName("Expedit Process")
                .orElseThrow(() -> new ResourceNotFoundException("Expedit Process"));

        CompanyServices companyServices = companyServiceRepository
                .findByCompanyAndServiceMaster(company, serviceMaster)
                .orElse(new CompanyServices());

        companyServices.setCompany(company);
        companyServices.setServiceMaster(serviceMaster);
        companyServices.setTrueFalse(required);
        companyServices.setServiceCreatedDate(LocalDateTime.now());
        companyServices.setServicePrice(serviceMaster.getServicePrice());
        companyServices.setStatus("wait");

        companyServiceRepository.save(companyServices);
        company.setIsExpeditedServiceSelected(required);
        companyRepository.save(company);

        return new CompanyResponseDto("Expedited Service Added Successfully", companyId);
    }

    @Transactional
    public CompanyResponseDto addCreationService(Long companyId) {
        log.info("Adding Creation Service for company ID: {}", companyId);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));

        ServiceMaster serviceMaster = serviceMasterRepository.findByServiceName("State Fee")
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: State Fee"));

        StateFee stateFee = stateFeeRepository.findByState(company.getState())
                .orElseThrow(() -> new ResourceNotFoundException("State Not Found"));

        Optional<CompanyServices> existingService = companyServiceRepository.findByCompanyAndServiceMaster(company,
                serviceMaster);

        CompanyServices companyServices = existingService.orElse(new CompanyServices());
        companyServices.setCompany(company);
        companyServices.setServiceMaster(serviceMaster);
        companyServices.setTrueFalse(true);
        companyServices.setServicePrice(stateFee.getStateFee());
        companyServices.setServiceCreatedDate(LocalDateTime.now());
        companyServices.setStatus("wait");

        companyServiceRepository.save(companyServices);

        return new CompanyResponseDto("Creation Service Added Successfully", companyId);
    }

    @Transactional
    public CompanyResponseDto addRegisterAgentService(Company company, Boolean isOurs) {
        log.info("Adding RegisterAgent Service for company ID: {}", company.getCompanyId());

        ServiceMaster serviceMaster = serviceMasterRepository.findByServiceName("Registered Agent Fee")
                .orElseThrow(() -> new ResourceNotFoundException("Registered Agent Fee Service NotFound"));

        Optional<CompanyServices> existingService = companyServiceRepository.findByCompanyAndServiceMaster(company,
                serviceMaster);

        CompanyServices companyServices = existingService.orElse(new CompanyServices());
        companyServices.setCompany(company);
        companyServices.setServiceMaster(serviceMaster);
        companyServices.setServiceCreatedDate(LocalDateTime.now());
        companyServices.setTrueFalse(isOurs);
        companyServices.setServicePrice(serviceMaster.getServicePrice());
        companyServices.setStatus("wait");

        companyServiceRepository.save(companyServices);

        return new CompanyResponseDto("RegisterAgent Service Added Successfully", company.getCompanyId());
    }

    @Transactional(readOnly = true)
    public List<FormationStatus> getRelevantStatuses() {
        log.info("Fetching relevant formation statuses");
        return repo.findByIsActiveTrueAndStatusFormationStatusNameNotIn(List.of("Initial", "Paid"));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAuditEvent(CompanyAuditEvent event) {
        addCreationService(event.companyId());
        try {
            companyFillingAuditService.setJsonDetails(event.companyId(), event.loginUserId(), event.notes());
        } catch (Exception e) {
            log.error("Failed to set JSON details for audit event: {}", e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public FormationStatusPagedResponse getRelevantStatusesPaged(Pageable pageable, String status, String state,
            String companyName, Boolean isExpedited) {
        List<String> excluded = List.of("Initial", "Paid");

        Page<FormationStatus> page = repo.searchFormationStatuses(
                status, state, companyName, isExpedited, excluded, pageable);

        List<StatusCountProjection> counts = repo.countByStatusExcluding(excluded);
        Map<String, Long> statusCounts = counts.stream()
                .collect(Collectors.toMap(StatusCountProjection::getStatusFormationStatusName,
                        StatusCountProjection::getCount));

        FormationStatusPagedResponse response = new FormationStatusPagedResponse();
        response.setPage(page);
        response.setStatusCounts(statusCounts);

        return response;
    }

    @Transactional
    public CompanyResponseDto saveManagementStyle(String managementStyle, Long companyId) {
        log.info("Saving Management Style for company ID: {}", companyId);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));

        company.setManagementStyle(managementStyle);
        company.setLastUpdated(LocalDateTime.now());

        Company updatedCompany = companyRepository.save(company);

        if (updatedCompany.getManagementStyle() == null) {
            return new CompanyResponseDto("Name Not Updated", null);
        }

        return new CompanyResponseDto("Company Management Style Saved Successfully", updatedCompany.getCompanyId());
    }

    @Transactional(readOnly = true)
    public Page<CompanySlim> search(List<String> principalActivityList, List<String> states,
            LocalDate effectiveFrom, LocalDate effectiveTo,
            String search, Long loginUserId, Pageable pageable) {
        // Treat empty list as null for the JPQL
        if (states != null && states.isEmpty())
            states = null;
        if (principalActivityList != null && principalActivityList.isEmpty())
            principalActivityList = null;
        return companyRepository.search(principalActivityList, states, effectiveFrom, effectiveTo, search, loginUserId,
                pageable);
    }

    @Transactional(readOnly = true)
    public CompanyLiteDto getLiteById(Long companyId) {
        Company c = companyRepository.findLiteById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("Company not found: " + companyId));
        return mapper.toDto(c);
    }

    // ==================== Private Helper Methods ====================

    /**
     * Transitions the company to a new formation status by deactivating the current
     * active status
     * and creating a new active status.
     */
    private void transitionFormationStatus(Company company, LoginUser loginUser,
            FormationStatusMaster newStatusMaster) {
        // Deactivate current active status if exists
        formationStatusRepository.findFirstByCompanyIdAndIsActiveTrue(company.getCompanyId())
                .ifPresent(currentStatus -> {
                    currentStatus.setIsActive(false);
                    formationStatusRepository.save(currentStatus);
                });

        // Create and save new active status
        FormationStatus newStatus = new FormationStatus();
        newStatus.setCompany(company);
        newStatus.setLoginUser(loginUser);
        newStatus.setStatus(newStatusMaster);
        newStatus.setStatusDate(LocalDate.now());
        newStatus.setCreatedOn(LocalDateTime.now());
        newStatus.setIsActive(true);
        formationStatusRepository.save(newStatus);
    }

    /**
     * Updates the registered agent details for a company.
     */
    private void updateRegisteredAgent(Company company, RegisteredAgentDto agent) {
        RegisteredAgent registeredAgent = registeredAgentRepository.findByCompany(company)
                .orElseThrow(() -> new ResourceNotFoundException("Registered Agent not found"));

        if (isValidValue(agent.getFirstName())) {
            registeredAgent.setFirstName(agent.getFirstName());
        }
        if (isValidValue(agent.getLastName())) {
            registeredAgent.setLastName(agent.getLastName());
        }
        if (isValidValue(agent.getStreetAddress1())) {
            registeredAgent.setStreetAddress1(agent.getStreetAddress1());
        }
        if (isValidValue(agent.getStreetAddress2())) {
            registeredAgent.setStreetAddress2(agent.getStreetAddress2());
        }
        if (isValidValue(agent.getCity())) {
            registeredAgent.setCity(agent.getCity());
        }
        if (isValidValue(agent.getState())) {
            registeredAgent.setState(agent.getState());
        }
        if (isValidValue(agent.getZipCode())) {
            registeredAgent.setZipCode(agent.getZipCode());
        }

        registeredAgentRepository.save(registeredAgent);
    }

    /**
     * Sends the company formation in progress email notification.
     */
    private void sendFormationInProgressEmail(Company company) {
        boolean emailSent = sendEmailService.sendCompanyFormationInProgressEmail(
                ServiceConstants.FROM_EMAIL,
                company.getLoginUser().getEmail(),
                ServiceConstants.IN_PROGRESS_SUBJECT,
                company.getLoginUser().getFirstName(),
                company.getCompanyName(),
                company.getState(),
                LocalDateTime.now().toString());
        if (!emailSent) {
            log.error("Failed to send email notification for company formation in progress");
        }
    }

    /**
     * Checks if a string value is valid (not null, not empty, and not the Swagger
     * default placeholder).
     */
    private boolean isValidValue(String value) {
        return value != null && !value.isBlank() && !SWAGGER_DEFAULT.equalsIgnoreCase(value);
    }
}
