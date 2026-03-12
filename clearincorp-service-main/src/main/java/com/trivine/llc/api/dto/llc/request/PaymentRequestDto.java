package com.trivine.llc.api.dto.llc.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequestDto {
    private Long amount;
    private String name;
    @Email(regexp = ".+[@].+[\\.].+", message = "Email should be valid")
    @Size(max = 255)
    private String email;
}
