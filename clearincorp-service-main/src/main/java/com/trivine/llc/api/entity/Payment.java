package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "payment")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @Column(name = "payment_amount", nullable = false)
    private Double paymentAmount;


    @Column(name = "payment_method")
    @Size(max = 255)
    private String paymentMethod;

    @Column(name = "currency", nullable = false, length = 10)
    @Size(max = 10)
    private String currency;

    @Column(name = "status", nullable = false, length = 20)
    @Size(max = 20)
    private String status;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "transaction_id", nullable = false)
    @Size(max = 255)
    private String transactionId;

    @Column(name = "invoice_id", nullable = false, unique = true)
    @Size(max = 255)
    private String invoiceId;

    @Column(name = "payment_blob", columnDefinition = "JSON")
    private String paymentBlob;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CompanyServices> companyServices;
}
