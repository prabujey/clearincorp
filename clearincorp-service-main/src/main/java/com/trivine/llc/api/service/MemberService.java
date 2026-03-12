package com.trivine.llc.api.service;

import com.trivine.llc.api.dto.llc.request.MemberMemberDto;
import com.trivine.llc.api.dto.response.CompanyResponseDto;
import com.trivine.llc.api.entity.Company;
import com.trivine.llc.api.entity.Member;
import com.trivine.llc.api.exception.ResourceNotFoundException;
import com.trivine.llc.api.mapper.MemberMapper;
import com.trivine.llc.api.repository.CompanyRepository;
import com.trivine.llc.api.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;



@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final CompanyRepository companyRepository;
    private final MemberMapper memberMapper;


    @Transactional
    public CompanyResponseDto saveAllMembers(List<MemberMemberDto> memberDtos, long companyId) {
        Company company = companyRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));
        memberRepository.deleteByCompany(company);

        List<Member> members = memberMapper.toEntityList(memberDtos);
        members.forEach(member -> member.setCompany(company));
        memberRepository.saveAll(members);




        return new CompanyResponseDto("Member Added", company.getCompanyId());
    }

}

