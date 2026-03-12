package com.trivine.llc.api.dto.response;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentResponseDto {

    private String status;
    private String message;
    private String clientSecret;
}