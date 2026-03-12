package com.trivine.llc.api.dto.request;

import com.trivine.llc.api.dto.Priority;
import com.trivine.llc.api.dto.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class BulkAdminAssignmentRequest {
    // master fields
    @NotBlank
    private String taskTitle;
    private String description;
    private Priority priority;
    private LocalDate dueDate;

    @NotNull
    private Long createdById;

    // scope
    private List<Long> assigneeRole;                // e.g., 3L SuperFiler, 4L Vendor (optional)
    private List<Long> assigneeUserIds; // optional explicit users
    private List<Long> companyIds; // companies to assign against

    // default status
    private TaskStatus initialStatus = TaskStatus.PENDING; // pending
}
