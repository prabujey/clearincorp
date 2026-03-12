package com.trivine.llc.api.dto;

import com.trivine.llc.api.dto.response.PagedResponse;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalTaskSearchResponse {
    // Your existing paginated response
    private PagedResponse<PersonalTaskResponseDto> pageData;

    // The new statistical data
    private PersonalTaskStatsDto stats;
}
