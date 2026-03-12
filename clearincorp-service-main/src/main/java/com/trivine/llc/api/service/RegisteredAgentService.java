package com.trivine.llc.api.service;

import com.trivine.llc.api.constants.ServiceConstants;
import com.trivine.llc.api.dto.llc.request.RegisteredAgentDto;
import com.trivine.llc.api.dto.response.CompanyResponseDto;
import com.trivine.llc.api.entity.Company;
import com.trivine.llc.api.entity.RegisteredAgent;
import com.trivine.llc.api.entity.RegisteredAgentMaster;
import com.trivine.llc.api.exception.ResourceNotFoundException;
import com.trivine.llc.api.repository.RegisteredAgentMasterRepository;
import com.trivine.llc.api.repository.RegisteredAgentRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Slf4j
@RequiredArgsConstructor
public class RegisteredAgentService {

    private final RegisteredAgentRepository registeredAgentRepository;
    private final RegisteredAgentMasterRepository registeredAgentMasterRepository;
    private final CompanyService companyService;

    @Transactional
    public CompanyResponseDto saveOrUpdate(@Valid RegisteredAgentDto dto, Long companyId, Boolean isOurs) {
        if (dto == null || companyId == null) {
            throw new IllegalArgumentException("DTO and Company ID must not be null");
        }

        Company company = new Company();
        company.setCompanyId(companyId);

        RegisteredAgent agent = registeredAgentRepository.findByCompany(company)
                .orElseGet(RegisteredAgent::new);

        agent.setCompany(company);
        agent.setFirstName(dto.getFirstName());
        agent.setLastName(dto.getLastName());
        agent.setStreetAddress1(dto.getStreetAddress1());
        agent.setStreetAddress2(dto.getStreetAddress2());
        agent.setCity(dto.getCity());
        agent.setState(dto.getState());
        agent.setZipCode(dto.getZipCode());
        agent.setCountry(ServiceConstants.DEFAULT_COUNTRY);
        agent.setEmail(dto.getEmail());
        agent.setPhoneNumber(dto.getPhoneNumber());
        agent.setUseAddress(isOurs);

        registeredAgentRepository.save(agent);
        CompanyResponseDto companyResponseDto = companyService.addRegisterAgentService(company, isOurs);
        // ⬇️ If condition true -> rollback
        if (!"RegisterAgent Service Added Successfully".equals(companyResponseDto.getMessage())) {
            throw new RuntimeException("RegisterAgent service failed, rolling back transaction");
        }
        log.info("Registered agent saved for companyId: {}", companyId);
        return new CompanyResponseDto("Registered agent saved successfully for companyId: ", companyId);
    }

    public RegisteredAgentDto getOneActiveAgentByState(String state) {
        if (state == null || state.trim().isEmpty()) {
            throw new IllegalArgumentException("State must not be null or empty");
        }

        RegisteredAgentMaster agent = registeredAgentMasterRepository
                .findFirstByStateAndIsActiveTrue(state)
                .orElseThrow(() -> new ResourceNotFoundException("No active agent found for state: " + state));

        return new RegisteredAgentDto(
                agent.getRegAgentFirstName(),
                agent.getRegAgentSecondName(),
                agent.getStreetAddress1(),
                agent.getStreetAddress2(),
                agent.getCity(),
                agent.getState(),
                agent.getZipCode(),
                agent.getCountry(),
                agent.getEmail(),
                agent.getPhoneNumber(),
                true
        );
    }
}