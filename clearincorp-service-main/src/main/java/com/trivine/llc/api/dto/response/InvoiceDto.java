package com.trivine.llc.api.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class InvoiceDto {
        private Long id;
        private Long companyId;
        private String invoiceId;
        private String status;
        private String billFrom;
        private String billFromEmail;
        private String billFromAddress;
        private String billFromPhone;
        private String state;
        private String billTo;
        private String billToEmail;
        private String billToAddress;
        private String billToPhone;
        private LocalDateTime orderDate;
        private List<InvoiceItemDto> orders;
        private Double grandTotal;
}