package com.trivine.llc.api.service;

import com.trivine.llc.api.dto.BusinessDetailsDto;
import com.trivine.llc.api.dto.llc.request.*;
import com.trivine.llc.api.dto.response.UserProgressDto;
import com.trivine.llc.api.dto.response.UserProgressResponseDto;
import com.trivine.llc.api.entity.*;
import com.trivine.llc.api.mapper.EinDetailsMapper;
import com.trivine.llc.api.mapper.ManagerMemberMapper;
import com.trivine.llc.api.mapper.MemberMapper;
import com.trivine.llc.api.repository.*;
import com.trivine.llc.api.repository.projection.CompanyProgressProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserWizardService {

    private final CompanyRepository companyRepository;

    private final MemberMapper memberMapper;
    private final ManagerMemberMapper managerMemberMapper;
    private final AsyncCompanyService asyncCompanyService;
    private final FormationStatusRepository formationStatusRepository;
    private final EinDetailsMapper einDetailsMapper;


    public List<UserProgressResponseDto> getUserProgress(Long loginUserId) {
        List<FormationStatus> statuses =
                formationStatusRepository.findLatestActiveStatusByLoginUserId(loginUserId);

        return statuses.stream().map(fs -> {
            Company company=fs.getCompany();
            UserProgressResponseDto dto = new UserProgressResponseDto();
            dto.setCompanyId(company.getCompanyId());
            dto.setCompanyName(company.getCompanyName());
            dto.setState(company.getState());
            String statusName=fs.getStatus().getFormationStatusName();
            dto.setStatusName(statusName);
            dto.setLlcName(company.getSuffixMaster() != null ? company.getSuffixMaster().getSuffix() : "");
            int step = 0;
            if (company.getCompanyDesc() != null) step = 1;
            if (Boolean.TRUE.equals(company.getRegForm1())) step = 2;
            if (Boolean.TRUE.equals(company.getRegForm2())) step = 3;
            if(!statusName.equals("initial") && !statusName.equals("paid") )step=4;
            dto.setStep(step);

            return dto;
        }).toList();

    }
    public List<UserProgressResponseDto> getUserProgressNew(Long loginUserId) {
        return formationStatusRepository.findLatestProgressByLoginUserId(loginUserId);
    }



    public UserProgressDto getUserProgressDto(Long companyId,Boolean isHelper) throws ExecutionException, InterruptedException, TimeoutException {
        UserProgressDto dto = new UserProgressDto();
        dto.setCompanyId(companyId);
        dto.setStep8(null);
        dto.setStep9(null);
        dto.setStep10(null);


            CompletableFuture<Optional<Company>> companyFuture = asyncCompanyService.getCompany(companyId);
            CompletableFuture<Optional<CompanyPrincipal>> principalFuture = asyncCompanyService.getPrincipal(companyId);
            CompletableFuture<Optional<List<ManagerMember>>> managerFuture = asyncCompanyService.getManagerMembers(companyId);
            CompletableFuture<Optional<List<Member>>> memberFuture = asyncCompanyService.getMembers(companyId);
            CompletableFuture<Optional<RegisteredAgent>> agentFuture = asyncCompanyService.getAgentByCompanyId(companyId);
            CompletableFuture<Optional<List<CompanyServices>>> servicesFuture = asyncCompanyService.getCompanyServices(companyId);
            CompletableFuture<Optional<FormationStatus>> statusFuture=asyncCompanyService.getFormationStatus(companyId);
            CompletableFuture<Optional<EinDetails>> einfuture=asyncCompanyService.getEinDetails(companyId);


            CompletableFuture.allOf(companyFuture, principalFuture, agentFuture, servicesFuture, managerFuture, memberFuture)
                    .get(15, TimeUnit.SECONDS);
            Optional<FormationStatus> formationStatusOptional=statusFuture.join();
        formationStatusOptional.ifPresent(formationStatus -> dto.setStatus(formationStatus.getStatus().getFormationStatusName()));


            Optional<EinDetails> einDetailsOptional=einfuture.join();
            if(einDetailsOptional.isPresent()){
                EinDetails einDetails=einDetailsOptional.get();
                if(!isHelper) einDetails.setSsnIdCipherJson(null);
                dto.setEinDetailsDto(einDetailsMapper.toDto(einDetails));
            }
            Optional<Company> companyOpt = companyFuture.join();
            if (companyOpt.isEmpty()) return dto;

            Company company = companyOpt.get();
            dto.setStep1(company.getState());

            if (company.getCompanyName() != null) {
                CompanyRequestDto nameDto = new CompanyRequestDto();
                nameDto.setCompanyName(company.getCompanyName());
                nameDto.setLlcSuffix(company.getSuffixMaster() != null ? company.getSuffixMaster().getSuffix() : null);
                dto.setStep2(nameDto);
            } else return dto;

            if (company.getCompanyEffectiveDate() != null) {
                dto.setStep3(company.getCompanyEffectiveDate());
            } else return dto;

            if (company.getCompanyDesc() != null) {
                BusinessDetailsDto businessDetailsDto=new BusinessDetailsDto();
                businessDetailsDto.setTradeName(company.getTradeName());
                businessDetailsDto.setBusinessDescription(company.getCompanyDesc());
                businessDetailsDto.setCompanyId(companyId);
                businessDetailsDto.setPrincipalActivity(company.getPrincipalActivity());
                dto.setStep4(businessDetailsDto);
            } else return dto;

            Optional<CompanyPrincipal> principalOpt = principalFuture.join();
            if (principalOpt.isEmpty()) return dto;
            CompanyPrincipal cp = principalOpt.get();
            dto.setStep5(new CompanyPrimaryContactDto(cp.getFirstName(), cp.getLastName(), cp.getEmail(), cp.getPhoneNumber()));

            Optional<RegisteredAgent> agentOpt = agentFuture.join();
            if (agentOpt.isEmpty()) return dto;
            RegisteredAgent ra = agentOpt.get();
            dto.setStep6(RegisteredAgentDto.builder()
                    .firstName(ra.getFirstName())
                    .lastName(ra.getLastName())
                    .streetAddress1(ra.getStreetAddress1())
                    .streetAddress2(ra.getStreetAddress2())
                    .city(ra.getCity())
                    .state(ra.getState())
                    .zipCode(ra.getZipCode())
                            .useAddress(ra.getUseAddress())
                    .build());

            Optional<List<CompanyServices>> serviceOpt = servicesFuture.join();
            if (serviceOpt.isEmpty()) return dto;

            dto.setStep7(Boolean.TRUE.equals(company.getRegForm1()));
            if (!dto.getStep7()) return dto;

            ProcessingChargesDto charges = new ProcessingChargesDto();
            BigDecimal total = BigDecimal.ZERO;

            for (CompanyServices cs : serviceOpt.get()) {
                String name = cs.getServiceMaster().getServiceName();
                Boolean tf = cs.getTrueFalse();
                Payment payment=cs.getPayment();
                BigDecimal price = cs.getServicePrice();

                if (name.equals("EIN Registration")) {
                    dto.setStep8(tf);
                    if(company.getRegForm2()!=null && company.getRegForm2()) {
                        if (tf && payment!=null) {
                            charges.setFileForEin(price);
                            total = total.add(price);
                        }
                    }
                    else{
                        if (tf) {
                            charges.setFileForEin(price);
                            total = total.add(price);
                        }
                    }
                }
                if (name.equals("Operating Agreement")) {
                    dto.setStep9(tf);
                    if(company.getRegForm2()!=null && company.getRegForm2()) {
                        if (tf && payment!=null) {
                            charges.setOperatingAgreement(price);
                            total = total.add(price);
                        }
                    }
                    else {
                        if (tf) {
                            charges.setOperatingAgreement(price);
                            total = total.add(price);
                        }
                    }
                }
                if (name.equals("Expedit Process")) {
                    dto.setStep10(tf);
                    if(company.getRegForm2()!=null && company.getRegForm2()) {
                        if (tf && payment!=null) {
                            charges.setExpediteRequired(price);
                            total = total.add(price);
                        }
                    }
                    else {
                        if (tf) {
                            charges.setExpediteRequired(price);
                            total = total.add(price);
                        }
                    }
                }
                if (name.equals("State Fee")) {
                    charges.setStateFee(price);
                    total = total.add(price);
                }
                if(name.equals("Registered Agent Fee")){
                    charges.setRegisterAgentFee(price);
                    total = total.add(price);
                }
            }

            charges.setTotalCharges(total);
            dto.setStep11(charges);
            if (company.getRegForm2()==null|| !company.getRegForm2()) return dto;
            dto.setStep12(true);

            if(company.getManagementStyle()==null)return dto;
            dto.setManagementStyle(company.getManagementStyle());

            Optional<List<ManagerMember>> managerOpt = managerFuture.join();
            dto.setStep13a(managerOpt.map(managerMemberMapper::toDtoList).orElse(Collections.emptyList()));

            Optional<List<Member>> memberOpt = memberFuture.join();
            if (memberOpt.isEmpty()) return dto;
            dto.setStep13b(memberOpt.map(memberMapper::toDtoList).orElse(Collections.emptyList()));


        if (company.getStreetAddress1() != null) {
                CompanyMailingAttributesDto mail = new CompanyMailingAttributesDto();
                mail.setStreetAddress1(company.getStreetAddress1());
                mail.setStreetAddress2(company.getStreetAddress2());
                mail.setCity(company.getCity());
                mail.setZipCode(company.getZipCode());
                mail.setUseAddress(company.getUseAddress());
                dto.setStep14(mail);
            } else return dto;
        dto.setStep15(Boolean.TRUE.equals(company.getRegForm3()));
        return dto;
    }

    public UserProgressDto getUserProgressDtoOptimized(Long companyId, Boolean isHelper) {

        UserProgressDto dto = new UserProgressDto();
        dto.setCompanyId(companyId);
        dto.setStep8(null);
        dto.setStep9(null);
        dto.setStep10(null);

        // IMPORTANT: repository must return List<CompanyProgressProjection>
        // List<CompanyProgressProjection> rows = companyRepository.findCompanyProgressRows(companyId);
        List<CompanyProgressProjection> rows = companyRepository.findCompanyProgressRows(companyId);

        if (rows == null || rows.isEmpty()) return dto;

        // Company will be same in all rows
        Company company = rows.get(0).getCompany();
        if (company == null) return dto;

        // ---- pick Principal (first non-null) ----
        CompanyPrincipal cp = rows.stream()
                .map(CompanyProgressProjection::getCompanyPrincipal)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        // ---- pick Registered Agent (first non-null) ----
        RegisteredAgent ra = rows.stream()
                .map(CompanyProgressProjection::getRegisteredAgent)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        // ---- pick FormationStatus (latest by statusDate, then id fallback) ----
        FormationStatus fs = rows.stream()
                .map(CompanyProgressProjection::getFormationStatus)
                .filter(Objects::nonNull)
                .max(Comparator
                        .comparing(FormationStatus::getStatusDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(FormationStatus::getCompanyStatusId, Comparator.nullsLast(Comparator.naturalOrder()))
                )
                .orElse(null);

        if (fs != null && fs.getStatus() != null) {
            dto.setStatus(fs.getStatus().getFormationStatusName());
        }

        // ---- pick EinDetails (latest by id fallback) ----
        EinDetails ein = rows.stream()
                .map(CompanyProgressProjection::getEinDetails)
                .filter(Objects::nonNull)
                .max(Comparator.comparing(EinDetails::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);

        if (ein != null) {
            var einDto = einDetailsMapper.toDto(ein);

            // DO NOT mutate entity (ein.setSsnIdCipherJson(null)) ❌
            // Null sensitive field on DTO only (adjust according to your DTO field name if needed)
            if (!Boolean.TRUE.equals(isHelper)) {
                // einDto.setSsnIdCipherJson(null);
            }
            dto.setEinDetailsDto(einDto);
        }

        // ---------------- your same step mapping ----------------

        dto.setStep1(company.getState());

        if (company.getCompanyName() != null) {
            CompanyRequestDto nameDto = new CompanyRequestDto();
            nameDto.setCompanyName(company.getCompanyName());
            nameDto.setLlcSuffix(company.getSuffixMaster() != null ? company.getSuffixMaster().getSuffix() : null);
            dto.setStep2(nameDto);
        } else return dto;

        if (company.getCompanyEffectiveDate() != null) {
            dto.setStep3(company.getCompanyEffectiveDate());
        } else return dto;

        if (company.getCompanyDesc() != null) {
            BusinessDetailsDto businessDetailsDto = new BusinessDetailsDto();
            businessDetailsDto.setTradeName(company.getTradeName());
            businessDetailsDto.setBusinessDescription(company.getCompanyDesc());
            businessDetailsDto.setCompanyId(companyId);
            businessDetailsDto.setPrincipalActivity(company.getPrincipalActivity());
            dto.setStep4(businessDetailsDto);
        } else return dto;

        // Principal (from projection)
        if (cp == null) return dto;
        dto.setStep5(new CompanyPrimaryContactDto(cp.getFirstName(), cp.getLastName(), cp.getEmail(), cp.getPhoneNumber()));

        // Agent (from projection)
        if (ra == null) return dto;
        dto.setStep6(RegisteredAgentDto.builder()
                .firstName(ra.getFirstName())
                .lastName(ra.getLastName())
                .streetAddress1(ra.getStreetAddress1())
                .streetAddress2(ra.getStreetAddress2())
                .city(ra.getCity())
                .state(ra.getState())
                .zipCode(ra.getZipCode())
                .useAddress(ra.getUseAddress())
                .build());

        // Services (already fetched)
        List<CompanyServices> services = company.getCompanyServices();
        if (services == null || services.isEmpty()) return dto;

        dto.setStep7(Boolean.TRUE.equals(company.getRegForm1()));
        if (!dto.getStep7()) return dto;

        ProcessingChargesDto charges = new ProcessingChargesDto();
        BigDecimal total = BigDecimal.ZERO;

        for (CompanyServices cs : services) {
            if (cs == null || cs.getServiceMaster() == null) continue;

            String name = cs.getServiceMaster().getServiceName();
            Boolean tf = cs.getTrueFalse();
            Payment payment = cs.getPayment();
            BigDecimal price = cs.getServicePrice();

            if ("EIN Registration".equals(name)) {
                dto.setStep8(tf);
                if (Boolean.TRUE.equals(company.getRegForm2())) {
                    if (Boolean.TRUE.equals(tf) && payment != null) { charges.setFileForEin(price); total = total.add(price); }
                } else {
                    if (Boolean.TRUE.equals(tf)) { charges.setFileForEin(price); total = total.add(price); }
                }
            }

            if ("Operating Agreement".equals(name)) {
                dto.setStep9(tf);
                if (Boolean.TRUE.equals(company.getRegForm2())) {
                    if (Boolean.TRUE.equals(tf) && payment != null) { charges.setOperatingAgreement(price); total = total.add(price); }
                } else {
                    if (Boolean.TRUE.equals(tf)) { charges.setOperatingAgreement(price); total = total.add(price); }
                }
            }

            if ("Expedit Process".equals(name)) {
                dto.setStep10(tf);
                if (Boolean.TRUE.equals(company.getRegForm2())) {
                    if (Boolean.TRUE.equals(tf) && payment != null) { charges.setExpediteRequired(price); total = total.add(price); }
                } else {
                    if (Boolean.TRUE.equals(tf)) { charges.setExpediteRequired(price); total = total.add(price); }
                }
            }

            if ("State Fee".equals(name)) {
                charges.setStateFee(price);
                total = total.add(price);
            }

            if ("Registered Agent Fee".equals(name)) {
                charges.setRegisterAgentFee(price);
                total = total.add(price);
            }
        }

        charges.setTotalCharges(total);
        dto.setStep11(charges);

        if (!Boolean.TRUE.equals(company.getRegForm2())) return dto;
        dto.setStep12(true);

        if (company.getManagementStyle() == null) return dto;
        dto.setManagementStyle(company.getManagementStyle());

        // Managers
        dto.setStep13a(
                company.getManagers() == null
                        ? Collections.emptyList()
                        : managerMemberMapper.toDtoList(new ArrayList<>(company.getManagers()))
        );

        // Members
        if (company.getMembers() == null || company.getMembers().isEmpty()) return dto;
        dto.setStep13b(memberMapper.toDtoList(new ArrayList<>(company.getMembers())));

        // Mailing
        if (company.getStreetAddress1() != null) {
            CompanyMailingAttributesDto mail = new CompanyMailingAttributesDto();
            mail.setStreetAddress1(company.getStreetAddress1());
            mail.setStreetAddress2(company.getStreetAddress2());
            mail.setCity(company.getCity());
            mail.setZipCode(company.getZipCode());
            mail.setUseAddress(company.getUseAddress());
            dto.setStep14(mail);
        } else return dto;

        dto.setStep15(Boolean.TRUE.equals(company.getRegForm3()));
        return dto;
    }

}





