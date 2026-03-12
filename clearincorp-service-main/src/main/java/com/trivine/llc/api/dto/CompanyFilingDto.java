package com.trivine.llc.api.dto;

import lombok.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyFilingDto {
    private Long id;

    private LoginUserDto filer;

    private CompanyDto company;

    private LocalDate filingDate;

    @Digits(integer = 10, fraction = 2, message = "Payment amount must be a valid monetary value")
    private BigDecimal paymentAmount;

    @Size(max = 255, message = "Payer name must not exceed 255 characters")
    private String payerName;

    @Size(max = 100, message = "Payment method must not exceed 100 characters")
    private String paymentMethod;

    @Size(max = 100, message = "Transaction code must not exceed 100 characters")
    private String transactionCode;

    @Size(max = 255, message = "Payment evidence file path must not exceed 255 characters")
    private String paymentEvidenceFile;

    private LocalDateTime createdOn;

    private Boolean isActive;
}
