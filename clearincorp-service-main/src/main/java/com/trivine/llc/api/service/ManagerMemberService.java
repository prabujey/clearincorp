package com.trivine.llc.api.service;

import com.trivine.llc.api.dto.llc.request.ManagerMemberDto;
import com.trivine.llc.api.dto.response.CompanyResponseDto;
import com.trivine.llc.api.entity.Company;
import com.trivine.llc.api.entity.ManagerMember;

import com.trivine.llc.api.exception.ResourceNotFoundException;
import com.trivine.llc.api.mapper.ManagerMemberMapper;
import com.trivine.llc.api.repository.CompanyRepository;
import com.trivine.llc.api.repository.ManagerMemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;




@Service
@RequiredArgsConstructor
public class ManagerMemberService {

    private final ManagerMemberRepository managerMemberRepository;
    private final CompanyRepository companyRepository;
    private final ManagerMemberMapper managerMemberMapper;


    @Transactional
    public CompanyResponseDto saveAllManagers(List<ManagerMemberDto> managerDtos, Long companyId) {
        Company company = companyRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));

        managerMemberRepository.deleteByCompany(company);

        List<ManagerMember> managers = managerMemberMapper.toEntityList(managerDtos);
        managers.forEach(manager -> manager.setCompany(company));

        managerMemberRepository.saveAll(managers);

        return new CompanyResponseDto("Manager Added", companyId);
    }

    @Transactional
    public CompanyResponseDto deleteManagers(Long companyId) {
        Company company = companyRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found for ID: " + companyId));

        managerMemberRepository.deleteByCompany(company);

        return new CompanyResponseDto("Old Managers deleted", companyId);
    }


}
