package com.trivine.llc.api.dto;

import com.trivine.llc.api.dto.response.BusinessRegistrationDto;
import com.trivine.llc.api.dto.response.PagedResponse;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BusinessAdminPageDto {
    private PagedResponse<BusinessRegistrationDto> page;
    private long verifiedCount;
    private long unverifiedCount;
    private long totalCount;
}