package com.trivine.llc.api.dto;

import jakarta.validation.constraints.Pattern;
import lombok.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyPrincipalDto {
    private Long companyPrincipalId;

    private Long companyId;

    @NotBlank(message = "First name is required")
    @Size(max = 60, message = "First name must not exceed 60 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 60, message = "Last name must not exceed 60 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(regexp = ".+[@].+[\\.].+", message = "Email should be valid")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    private String email;

    @Pattern(
            regexp = "^\\(\\d{3}\\) \\d{3}-\\d{4}$",
            message = "Phone number must be in the format (123) 456-7890"
    )    @Size(max = 255, message = "Phone number must not exceed 255 characters")
    private String phoneNumber;

    private LocalDateTime createdOn;

    private Boolean isActive;
}
