package com.trivine.llc.api.dto;

import lombok.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilingFailureDTO {
    private Long filingFailureId;

    @NotNull(message = "Company filing is required")
    private CompanyFilingDto companyFiling;

    @NotNull(message = "Failure category is required")
    private FilingFailureCategoryDTO filingFailureCategory;

    @Size(max = 65535, message = "Description too long")
    private String failureDescription;

    @Size(max = 65535, message = "Next steps too long")
    private String nextSteps;

    private LocalDateTime createdOn;

    private Boolean isActive;
}
