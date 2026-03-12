package com.trivine.llc.api.dto;


import com.trivine.llc.api.dto.response.PagedResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskAnalyticsResponse {

    // This will hold your paginated list of tasks
    private PagedResponse<AssignedTaskAnalyticsDto> tasks;

    // This will hold the aggregate counts
    private TaskStatusCounts statusCounts;
}
