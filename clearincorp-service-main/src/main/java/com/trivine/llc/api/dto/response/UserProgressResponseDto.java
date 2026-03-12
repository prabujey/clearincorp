package com.trivine.llc.api.dto.response;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProgressResponseDto {
    private Long companyId;
    private String CompanyName;
    private String llcName;
    private String State;
    private String statusName;
    private int step;
}
