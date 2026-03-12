package com.trivine.llc.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileUpdateDto {

    private Long loginUserId;

    private String  profileImageUrl;

    @Size(max = 60, message = "First name must not exceed 255 characters")
    private String firstName;

    @Size(max = 60, message = "Last name must not exceed 255 characters")
    private String lastName;

    @Email(regexp = ".+[@].+[\\.].+", message = "Email should be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Size(max = 14)
    @Pattern(regexp = "^(|\\(\\d{3}\\) \\d{3}-\\d{4})$")
    private String phoneNumber;

}
