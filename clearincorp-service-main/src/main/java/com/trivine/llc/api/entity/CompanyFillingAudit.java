package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter

@AllArgsConstructor
@NoArgsConstructor

@Table(name = "company_filing_audit")
public class CompanyFillingAudit{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_filing_audit_id", nullable = false)
    private Long companyFilingAuditId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "login_user_id", nullable = false)
    private Long loginUserId;

    @Lob
    @Column(name = "json", nullable = false, columnDefinition = "TEXT")
    private String json;

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "notes")
    @Size(max = 255)
    private String notes;



    public CompanyFillingAudit(Long companyId, Long loginUserId, String json) {
        this.companyId=companyId;
        this.loginUserId = loginUserId;
        this.json=json;
        this.isActive = true; // default

    }


}