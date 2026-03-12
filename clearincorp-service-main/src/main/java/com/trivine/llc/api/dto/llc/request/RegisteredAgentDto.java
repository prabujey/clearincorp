
package com.trivine.llc.api.dto.llc.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisteredAgentDto {

    @Size(max = 60)
    private String firstName;

    @Size(max = 60)
    private String lastName;

    @NotBlank
    @Size(max = 120)
    @Pattern(regexp = "^[\\p{L}\\p{N}\\s.'#\\-/,]+$",
            message = "Street may contain letters, numbers, spaces and . ' # - / ,")
    private String streetAddress1;

    @Size(max = 80)
    @Pattern(regexp = "^[\\p{L}\\p{N}\\s.'#\\-/,]*$",
            message = "Street 2 may contain letters, numbers, spaces and . ' # - / ,")
    private String streetAddress2;

    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{L}\\s.'\\-]+$",
            message = "City may contain letters, spaces, and . ' -")
    private String city;

    @NotBlank
    @Size(max = 50)
    private String state;

    @NotBlank
    private String zipCode;

    private String country;

    @Email(regexp = ".+[@].+[\\.].+", message = "Email should be valid")
    @Size(max = 255)
    private String email;

    @Pattern(
            regexp = "^\\(\\d{3}\\) \\d{3}-\\d{4}$",
            message = "Phone number must be in the format (123) 456-7890"
    )    @Size(max = 255)
    private String phoneNumber;
    private Boolean useAddress;


}
