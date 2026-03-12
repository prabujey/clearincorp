package com.trivine.llc.api.dto.llc.request;

import jakarta.persistence.Column;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberMemberDto {

    @Size(max = 60)
    private String firstName;

    @Size(max = 60)
    private String lastName;

    @Email(regexp = ".+[@].+[\\.].+", message = "Email should be valid")
    @Size(max = 255)
    private String email;

    @Pattern(
            regexp = "^\\(\\d{3}\\) \\d{3}-\\d{4}$",
            message = "Phone number must be in the format (123) 456-7890"
    )
    @Size(max = 255)
    private String phoneNumber;

    @NotBlank
    @Size(max = 120)
    @Pattern(regexp = "^[\\p{L}\\p{N}\\s.'#\\-/,]+$",
            message = "Street may contain letters, numbers, spaces and . ' # - / ,")
    private String streetAddress1;

    @Size(max = 80)
    @Pattern(regexp = "^[\\p{L}\\p{N}\\s.'#\\-/,]*$",
            message = "Street 2 may contain letters, numbers, spaces and . ' # - / ,")
    private String streetAddress2;

    @Size(max = 100)
    private String city;

    @Size(max = 50)
    private String state;

    @Size(max = 20)
    private String zipCode;

    @Size(max = 50)
    private String country;

    @NotBlank
    @Size(max = 100)
    private String ownership;

    private Boolean isEinResponsibleParty;

    private Boolean useAddress;
}
