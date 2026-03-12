package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "company_service")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyServices {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_service_id", nullable = false)
    private Long companyServiceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceMaster serviceMaster;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "status", nullable = false)
    @Size(max = 255)
    private String status;

    @Column(name = "service_price", nullable = false, precision = 38, scale = 2)
    private BigDecimal servicePrice;

    @Column(name = "service_created_date")
    private LocalDateTime serviceCreatedDate;

    @Column(name = "service_completion_date")
    private LocalDateTime serviceCompletionDate;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "true_false")
    private Boolean trueFalse = false;


    public boolean getTrueFalse(){
        return trueFalse;
    }

}
