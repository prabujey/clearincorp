package com.trivine.llc.api.dto;

import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PersonalTaskResponseDto {
    private UUID id;
    private String taskTitle;
    private Priority priority;
    private LocalDate dueDate;
    private boolean completed;
    private Instant createdOn;
    private Instant updatedOn;
    private Long loginUserId;
}
