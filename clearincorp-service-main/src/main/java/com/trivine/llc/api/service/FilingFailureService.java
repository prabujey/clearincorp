package com.trivine.llc.api.service;

import com.trivine.llc.api.dto.FilingFailureDTO;
import com.trivine.llc.api.entity.*;
import com.trivine.llc.api.exception.ResourceNotFoundException;
import com.trivine.llc.api.mapper.CompanyFilingMapper;
import com.trivine.llc.api.mapper.FilingFailureMapper;
import com.trivine.llc.api.repository.*;
import com.trivine.llc.api.service.utility.SendEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.trivine.llc.api.constants.ServiceConstants.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class FilingFailureService {

    private final FilingFailureRepository filingFailureRepository;
    private final FilingFailureMapper filingFailureMapper;
    private final FormationStatusRepository formationStatusRepository;
    private final FormationStatusMasterRepository formationStatusMasterRepository;
    private final CompanyRepository companyRepository;
    private final CompanyFilingRepository companyFilingRepository;
    private final CompanyFilingMapper companyFillingMapper;
    private final SendEmailService sendEmailService;
    private final FilingFailureCategoryRepository filingFailureCategoryRepository;


    public List<FilingFailureDTO> getAllFilingFailures() {
        log.info("Fetching all filing failures");
        return filingFailureRepository.findAll()
                .stream()
                .map(filingFailureMapper::toDto)
                .collect(Collectors.toList());
    }

    public FilingFailureDTO getFilingFailureById(long id) {
        log.info("Fetching filing failure by ID: {}", id);
        return filingFailureRepository.findById(id)
                .map(filingFailureMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Filing failure not found for ID: " + id));
    }

    public FilingFailureDTO createFilingFailure(FilingFailureDTO dto, Long companyId, Long loginId) {
        log.info("Creating new filing failure for company ID: {}", companyId);

        CompanyFiling companyFiling = companyFilingRepository.findLatestActiveByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("No active filing status found for company ID: " + companyId));

        dto.setCompanyFiling(companyFillingMapper.toDto(companyFiling));
        FilingFailure filingFailure = filingFailureMapper.toEntity(dto);
        filingFailure = filingFailureRepository.save(filingFailure);

        FormationStatus currentStatus = formationStatusRepository.findFirstByCompanyIdAndIsActiveTrue(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("No active formation status found for company ID: " + companyId));

        currentStatus.setIsActive(false);
        formationStatusRepository.save(currentStatus);

        Company company = companyRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found for ID: " + companyId));

        FormationStatusMaster statusMaster = formationStatusMasterRepository.findByFormationStatusName("failure")
                .orElseThrow(() -> new ResourceNotFoundException("Status 'failure' not found in master table"));

        FormationStatus newStatus = new FormationStatus();
        newStatus.setCompany(company);
        newStatus.setLoginUser(new LoginUser(loginId));
        newStatus.setStatus(statusMaster);
        newStatus.setStatusDate(LocalDate.now());
        newStatus.setCreatedOn(LocalDateTime.now());
        newStatus.setIsActive(true);
        formationStatusRepository.save(newStatus);
        FilingFailureCategory filingFailureCategory=filingFailureCategoryRepository.findByFilingFailureCategoryId(dto.getFilingFailureCategory().getFilingFailureCategoryId());
        LoginUser loginUser=company.getLoginUser();
        String email=loginUser.getEmail();
        String firstName=loginUser.getFirstName();
        String category=filingFailureCategory.getFilingFailureCategory();
        String failureDescription= filingFailure.getFailureDescription();
        String nextSteps= filingFailure.getNextSteps();
        String companyName=company.getCompanyName();


        boolean otpSent = sendEmailService.sendFilingFailureEmail(FROM_EMAIL, email, FILED_FAILED_SUBJECT,firstName,companyName,category,failureDescription,nextSteps);
        if (!otpSent) {
            log.warn("Failed to send OTP email to: {}", email);
            throw new IllegalStateException("Email Not Found or Failed to Send");
        }

        return filingFailureMapper.toDto(filingFailure);
    }


    public FilingFailureDTO updateFilingFailure(long id, FilingFailureDTO dto) {
        log.info("Updating filing failure ID: {}", id);

        if (!filingFailureRepository.existsById(id)) {
            throw new ResourceNotFoundException("Filing failure not found for ID: " + id);
        }

        dto.setFilingFailureId(id);
        FilingFailure filingFailure = filingFailureMapper.toEntity(dto);
        FilingFailure updated = filingFailureRepository.save(filingFailure);

        return filingFailureMapper.toDto(updated);
    }

    public void deleteFilingFailure(long id) {
        log.info("Deleting filing failure ID: {}", id);

        if (!filingFailureRepository.existsById(id)) {
            throw new ResourceNotFoundException("Filing failure not found for ID: " + id);
        }

        filingFailureRepository.deleteById(id);
    }

}


