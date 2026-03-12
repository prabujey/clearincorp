package com.trivine.llc.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusCounts {
    private long pending;
    private long done;
    private long ignored;
}
