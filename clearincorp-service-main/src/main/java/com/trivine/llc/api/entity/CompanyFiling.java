package com.trivine.llc.api.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Table(name = "company_filing")
public class CompanyFiling {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_filing_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "filer_id", referencedColumnName = "login_user_id", nullable = false)
    private LoginUser filer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", referencedColumnName = "company_id", nullable = false)
    private Company company;

    @Column(name = "filing_date", nullable = false)
    private LocalDate filingDate;

    @Column(name = "payment_amount", precision = 10, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "payer_name")
    @Size(max = 255)
    private String payerName;

    @Column(name = "payment_method", length = 100)
    @Size(max = 100)
    private String paymentMethod;

    @Column(name = "transaction_code", length = 100)
    @Size(max = 100)
    private String transactionCode;

    @Column(name = "payment_evidence_file")
    @Size(max = 255)
    private String paymentEvidenceFile;

    @Column(name = "created_on", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdOn;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @PrePersist
    protected void onCreate() {
        createdOn = LocalDateTime.now();
    }

}

