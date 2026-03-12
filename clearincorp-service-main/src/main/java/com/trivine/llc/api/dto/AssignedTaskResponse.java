package com.trivine.llc.api.dto;

import com.trivine.llc.api.dto.response.PagedResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignedTaskResponse {

    // Your existing paginated response
    private PagedResponse<TaskItemDto> pageData;

    // The new statistical data
    private AssignedTaskStatsDto stats;
}

