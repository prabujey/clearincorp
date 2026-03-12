package com.trivine.llc.api.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for the Task Analytics page.
 * This class MUST have an @AllArgsConstructor that matches the
 * order of fields in the repository's JPQL constructor query.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignedTaskAnalyticsDto {

    // --- From AssignedTask ---
    private String taskId;
    private String notes;
    private TaskStatus status;
    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;
    private List<String> attachmentKeys;

    // --- From LoginUser (Assignee) ---
    private String assigneeFirstName;
    private String assigneeLastName;
    private String assigneeEmail;

    // --- From Company ---
    private String companyName;
    private String state;// This will be null if task.companyId is null
}