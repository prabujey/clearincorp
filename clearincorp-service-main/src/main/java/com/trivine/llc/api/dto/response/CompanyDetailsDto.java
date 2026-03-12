package com.trivine.llc.api.dto.response;

import com.trivine.llc.api.dto.llc.request.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class CompanyDetailsDto {
    private String state;
    private CompanyRequestDto companyRequestDto;
    private LocalDate formationDate;
    private CompanyPrimaryContactDto companyPrimaryContactDto;
    private RegisteredAgentDto registeredAgentDto;
    private List<ManagerMemberDto> managerMemberDtoList;
    private List<MemberMemberDto> memberMemberDtoList;
    private CompanyMailingAttributesDto companyMailingAttributesDto;

}
