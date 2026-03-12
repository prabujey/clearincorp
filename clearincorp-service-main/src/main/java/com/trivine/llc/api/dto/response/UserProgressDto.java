package com.trivine.llc.api.dto.response;
import com.trivine.llc.api.dto.BusinessDetailsDto;
import com.trivine.llc.api.dto.EinDetailsDto;
import com.trivine.llc.api.dto.llc.request.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor


public class UserProgressDto {
    private Long companyId;
    private String step1;
    private CompanyRequestDto step2;
    private LocalDate step3;
    private BusinessDetailsDto step4;
    private CompanyPrimaryContactDto step5;
    private RegisteredAgentDto step6;
    private Boolean  step7;
    private Boolean step8;
    private Boolean step9;
    private Boolean step10;
    private ProcessingChargesDto step11;
    private Boolean step12;
    private List<ManagerMemberDto> step13a;
    private List<MemberMemberDto> step13b;
    private CompanyMailingAttributesDto step14;
    private Boolean step15;
    private EinDetailsDto einDetailsDto;
    private String managementStyle;
    private String status;
}