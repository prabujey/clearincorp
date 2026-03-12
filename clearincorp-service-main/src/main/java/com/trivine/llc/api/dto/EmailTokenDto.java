package com.trivine.llc.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailTokenDto {

    private Long tokenId;

    @NotBlank(message = "Email is required")
    @Email(regexp = ".+[@].+[\\.].+", message = "Email should be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Token value is required")
    @Size(min = 6, max = 6, message = "Token must be exactly 6 characters")
    private String tokenValue;

    private LocalDateTime createdOn;

    @Builder.Default
    private Boolean isUsed = false;
}
