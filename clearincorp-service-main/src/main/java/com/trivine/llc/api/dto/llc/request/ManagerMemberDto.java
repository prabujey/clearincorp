package com.trivine.llc.api.dto.llc.request;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagerMemberDto {

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
    )    @Size(max = 255)
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

    @Size(max = 255)
    private String city;

    @Size(max = 255)
    private String state;

    @Size(max = 255)
    private String zipCode;

    @Size(max = 255)
    private String country;

    @Column(name = "use_address")
    private Boolean useAddress;
}
