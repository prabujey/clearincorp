package com.trivine.llc.api.dto;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalTaskStatsDto {
    private long pendingCount;
    private long completedCount;
    private long overdueCount;
}