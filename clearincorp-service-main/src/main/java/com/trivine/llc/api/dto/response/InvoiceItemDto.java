package com.trivine.llc.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

public class InvoiceItemDto {
    private String itemName;
    private BigDecimal unitPrice;
    private Long units;
    private BigDecimal unitTotalPrice;
}
