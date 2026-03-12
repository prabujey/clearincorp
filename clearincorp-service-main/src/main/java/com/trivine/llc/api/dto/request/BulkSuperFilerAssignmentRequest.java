package com.trivine.llc.api.dto.request;


import com.trivine.llc.api.dto.Priority;
import com.trivine.llc.api.dto.TaskStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BulkSuperFilerAssignmentRequest {

    @NotNull(message = "createdById is required")
    private Long createdById;

    @NotBlank(message = "taskTitle is required")
    private String taskTitle;

    private String description;
    private Priority priority;
    private LocalDate dueDate;

    // default PENDING if not provided
    private TaskStatus initialStatus = TaskStatus.PENDING;

    // Assign to multiple users, each with its own company
    @NotEmpty(message = "targets must not be empty")
    @Valid
    private List<AssigneeTarget> targets;
}

