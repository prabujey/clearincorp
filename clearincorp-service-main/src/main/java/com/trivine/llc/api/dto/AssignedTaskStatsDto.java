package com.trivine.llc.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignedTaskStatsDto {
    private long pendingCount;
    private long completedCount;
    private long overdueCount;
    private long ignoredCount;
}
