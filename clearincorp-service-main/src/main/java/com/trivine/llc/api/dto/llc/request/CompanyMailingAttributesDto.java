package com.trivine.llc.api.dto.llc.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class CompanyMailingAttributesDto {

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
    private String zipCode;

    private Boolean useAddress;

}

