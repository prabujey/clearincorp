package com.trivine.llc.api.dto;

import lombok.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilingFailureCategoryDTO {
    private Integer filingFailureCategoryId;

    @NotBlank(message = "Category name is required")
    @Size(max = 255, message = "Category name must not exceed 255 characters")
    private String filingFailureCategory;

    private LocalDateTime createdOn;

    private Boolean isActive;
}
