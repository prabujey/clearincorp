package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Table(name = "filing_failure")
public class FilingFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "filing_failure_id", nullable = false)
    private Long filingFailureId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_filing_id", nullable = false)
    private CompanyFiling companyFiling;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "filing_failure_category_id", nullable = false)
    private FilingFailureCategory filingFailureCategory;

    @Column(name = "failure_description", columnDefinition = "TEXT")
    @Size(max = 255)
    private String failureDescription;

    @Column(name = "next_steps", columnDefinition = "TEXT")
    @Size(max = 255)
    private String nextSteps;

    @Column(name = "created_on", insertable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
