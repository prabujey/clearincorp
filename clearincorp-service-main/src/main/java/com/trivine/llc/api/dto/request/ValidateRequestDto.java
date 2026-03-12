package com.trivine.llc.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidateRequestDto {

    @NotBlank(message = "Email is required")
    @Email(regexp = ".+[@].+[\\.].+", message = "Email should be valid")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "Token is required")
    @Size(min = 6, max = 6, message = "Token must be exactly 6 characters")
    private String token;
}
