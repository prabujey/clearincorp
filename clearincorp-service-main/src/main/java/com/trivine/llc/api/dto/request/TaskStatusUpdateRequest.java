package com.trivine.llc.api.dto.request;

import com.trivine.llc.api.dto.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskStatusUpdateRequest {
    @NotNull
    private String taskId;
    @NotNull
    private TaskStatus status;
    private String notes;
}

