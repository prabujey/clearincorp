package com.trivine.llc.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AssignedTaskDto {
    private String id;
    private String masterId;
    private Long companyId;
    private Long assigneeId;
    private TaskStatus status;
    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;
}

