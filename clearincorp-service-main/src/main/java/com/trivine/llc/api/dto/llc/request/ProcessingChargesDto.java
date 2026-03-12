package com.trivine.llc.api.dto.llc.request;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessingChargesDto {

    private BigDecimal fileForEin;

    private BigDecimal operatingAgreement;

    private BigDecimal expediteRequired;

    private BigDecimal stateFee;

    private BigDecimal registerAgentFee;

    private BigDecimal totalCharges;
}
