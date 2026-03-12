package com.trivine.llc.api.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AssignedTaskMasterDto {
    private String id;
    private String taskTitle;
    private String description;
    private Long createdById;
    private Priority priority;
    private LocalDate dueDate;
    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;
    // NEW: persisted S3 keys
    private List<String> attachmentKeys;
}
