package com.trivine.llc.api.dto.llc.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;


@Data
@AllArgsConstructor
@NoArgsConstructor

public class CompanyPrimaryContactDto {
    @Size(max = 60)
    private String firstName;

    @Size(max = 60)
    private String lastName;

    @Email(regexp = ".+[@].+[\\.].+", message = "Email should be valid")
    @Size(max = 255) // match DB email field
    private String email;

    @Size(max = 255) // match DB phone field
    @Pattern(
            regexp = "^\\(\\d{3}\\) \\d{3}-\\d{4}$",
            message = "Phone number must be in the format (123) 456-7890"
    )
    private String phoneNumber;


}
