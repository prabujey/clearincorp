package com.trivine.llc.api.dto.request;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpRequestDTO {
    @NotBlank(message = "Email is required")
    @Email(regexp = ".+[@].+[\\.].+", message = "Email should be valid")
    private String email;

}

