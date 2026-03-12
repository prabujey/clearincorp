package com.trivine.llc.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskItemDto(
        String taskId,
        Long assigneeId,
        TaskStatus status,
        Long companyId,
        String notes,
        String masterId,
        String taskTitle,
        String description,
        Priority priority,
        LocalDate dueDate,
        LocalDateTime taskCreatedOn
) {}

