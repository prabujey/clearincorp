package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Table(name = "filing_failure_category")
public class FilingFailureCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "filing_failure_category_id", nullable = false)
    private Integer filingFailureCategoryId;

    @NotBlank(message = "Category name is required")
    @Size(max = 255)
    @Column(name = "filing_failure_category", nullable = false)
    private String filingFailureCategory;

    @Column(name = "created_on", insertable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
