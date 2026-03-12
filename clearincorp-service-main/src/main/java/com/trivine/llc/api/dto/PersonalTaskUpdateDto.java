package com.trivine.llc.api.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PersonalTaskUpdateDto {
    @NotBlank
    @Size(max = 300)
    private String taskTitle;

    @NotNull
    private Priority priority;

    @NotNull
    @FutureOrPresent(message = "Due date must be today or in the future")
    private LocalDate dueDate;

    @NotNull
    private Boolean completed;
}
