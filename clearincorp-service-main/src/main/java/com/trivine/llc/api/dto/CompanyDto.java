package com.trivine.llc.api.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class CompanyDto {
    private Long companyId;

    @NotNull(message = "Company name is required")
    @Size(max = 150, message = "Company name must not exceed 16=50 characters")
    private String companyName;

    private Long suffixId;

    private LocalDateTime companyEffectiveDate;

    @NotNull(message = "State is required")
    @Size(max = 255, message = "State must not exceed 255 characters")
    private String state;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String companyDesc;

    @Size(max = 255, message = "Street Address 1 must not exceed 255 characters")
    private String streetAddress1;

    @Size(max = 255, message = "Street Address 2 must not exceed 255 characters")
    private String streetAddress2;

    @Size(max = 255, message = "City must not exceed 255 characters")
    private String city;

    @Size(max = 255, message = "Zip code must not exceed 255 characters")
    private String zipCode;

    @Size(max = 255, message = "Country must not exceed 255 characters")
    private String country;

    @Email(regexp = ".+[@].+[\\.].+", message = "Email should be valid")
    @Size(max = 255, message = "Primary email must not exceed 255 characters")
    private String companyEmail1;

    @Email(regexp = ".+[@].+[\\.].+", message = "Email should be valid")
    @Size(max = 255, message = "Secondary email must not exceed 255 characters")
    private String companyEmail2;

    @Pattern(regexp = "^[0-9]{3} [0-9]{4} [0-9]{3}$", message = "Primary phone format is invalid")
    @Size(max = 255, message = "Primary phone must not exceed 255 characters")
    private String companyPhone1;

    @Pattern(regexp = "^[0-9]{3} [0-9]{4} [0-9]{3}$", message = "Secondary phone format is invalid")
    @Size(max = 255, message = "Secondary phone must not exceed 255 characters")
    private String companyPhone2;

    private Boolean regForm1;
    private Boolean regForm2;
    private Boolean regForm3;
    private Boolean regForm4;
    private String managementStyle;
    private Boolean useAddress;
    @Pattern(regexp = "^\\d{3}-\\d{2}-\\d{4}$",
            message = "SSN must be in format XXX-XX-XXXX")
    private String tradeName;
    private String principalActivity;
    private Boolean IsExpeditedServiceSelected;
}