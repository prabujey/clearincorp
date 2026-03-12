package com.trivine.llc.api.service;

import com.trivine.llc.api.constants.ServiceConstants;
import com.trivine.llc.api.dto.CompanyFilingDto;
import com.trivine.llc.api.entity.*;
import com.trivine.llc.api.exception.ResourceNotFoundException;
import com.trivine.llc.api.mapper.CompanyFilingMapper;
import com.trivine.llc.api.mapper.CompanyMapper;
import com.trivine.llc.api.mapper.LoginUserMapper;
import com.trivine.llc.api.repository.CompanyFilingRepository;
import com.trivine.llc.api.repository.CompanyRepository;
import com.trivine.llc.api.repository.FormationStatusMasterRepository;
import com.trivine.llc.api.repository.FormationStatusRepository;
import com.trivine.llc.api.service.utility.SendEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class CompanyFilingService {

    private final CompanyFilingRepository companyFilingRepository;
    private final CompanyFilingMapper mapper;
    private final FormationStatusRepository formationStatusRepository;
    private final FormationStatusMasterRepository formationStatusMasterRepository;
    private final CompanyMapper companyMapper;
    private final LoginUserMapper loginUserMapper;
    private final CompanyRepository companyRepository;
    private final SendEmailService sendEmailService;


    public List<CompanyFilingDto> getAll() {
        List<CompanyFiling> filings = companyFilingRepository.findAll();
        log.info("Retrieved all company filings, count: {}", filings.size());
        return filings.stream().map(mapper::toDto).collect(Collectors.toList());
    }
    public CompanyFilingDto getById(Long id) {
        CompanyFiling filing = companyFilingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company filing not found for ID: " + id));

        log.info("CompanyFiling found for ID: {}", id);
        return mapper.toDto(filing);
    }
    @Transactional
    public CompanyFilingDto create(CompanyFilingDto dto) {
        Long companyId = dto.getCompany().getCompanyId();
        Optional<CompanyFiling> existing = companyFilingRepository.findLatestActiveByCompanyId(companyId);
        existing.ifPresent(cf -> {
            cf.setIsActive(false);
            companyFilingRepository.save(cf);
            log.debug("Previous active CompanyFiling set to inactive for companyId: {}", companyId);
        });

        CompanyFiling entity = mapper.toEntity(dto);
        CompanyFiling saved = companyFilingRepository.save(entity);

        FormationStatus currentStatus = formationStatusRepository
                .findFirstByCompanyIdAndIsActiveTrue(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("No active formation status found for company ID: " + companyId));
        currentStatus.setIsActive(false);
        formationStatusRepository.save(currentStatus);

        FormationStatusMaster status = formationStatusMasterRepository
                .findByFormationStatusName("filed")
                .orElseThrow(() -> new ResourceNotFoundException("Status 'filed' not found in FormationStatusMaster"));

        FormationStatus newStatus = new FormationStatus();
        newStatus.setCompany(saved.getCompany());
        newStatus.setLoginUser(LoginUser.builder().loginUserId(dto.getFiler().getLoginUserId()).build());
        newStatus.setStatus(status);
        newStatus.setStatusDate(LocalDate.now());
        newStatus.setCreatedOn(LocalDateTime.now());
        newStatus.setIsActive(true);

        FormationStatus formationStatus = formationStatusRepository.save(newStatus);

        Company company = companyRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));

        String userName = Optional.ofNullable(company.getLoginUser())
                .map(LoginUser::getFirstName)
                .orElse("User");

        String formattedDate = formationStatus.getCreatedOn()
                .format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a"));

        boolean emailSent = sendEmailService.sendFilingSubmittedEmail(
                ServiceConstants.FROM_EMAIL,
                company.getLoginUser().getEmail(),
                ServiceConstants.FILED_SUBJECT,
                userName,
                company.getCompanyName(),
                formattedDate,
                company.getState()
        );

        if (!emailSent) {
            log.warn("Failed to send filing email to: {}", company.getLoginUser().getEmail());
        }

        log.info("Created new CompanyFiling and updated formation status for companyId: {}", companyId);
        return mapper.toDto(saved);
    }

    public CompanyFilingDto update(Long id, CompanyFilingDto dto) {
        CompanyFiling existing = companyFilingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CompanyFiling not found for ID: " + id));

        existing.setFiler(loginUserMapper.toEntity(dto.getFiler()));
        existing.setCompany(companyMapper.toEntity(dto.getCompany()));
        existing.setFilingDate(dto.getFilingDate());
        existing.setPaymentAmount(dto.getPaymentAmount());
        existing.setTransactionCode(dto.getTransactionCode());
        existing.setPaymentEvidenceFile(dto.getPaymentEvidenceFile());
        existing.setIsActive(dto.getIsActive());

        CompanyFiling updated = companyFilingRepository.save(existing);

        log.info("Updated CompanyFiling for ID: {}", id);
        return mapper.toDto(updated);
    }

    public void delete(Long id) {
        CompanyFiling filing = companyFilingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CompanyFiling not found for ID: " + id));

        filing.setIsActive(false);
        companyFilingRepository.save(filing);

        log.info("Soft-deleted CompanyFiling for ID: {}", id);
    }

}