package com.trivine.llc.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCompanyDto {

    private Long id;

    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String ein;

    @Pattern(
            regexp = "^\\(\\d{3}\\) \\d{3}-\\d{4}$",
            message = "Phone number must be in the format (123) 456-7890"
    )    @Size(max = 20)
    private String phoneNumber;

    @Email(regexp = ".+[@].+[\\.].+", message = "Email should be valid")
    @Size(max = 255)
    private String email;

    @Size(max = 255)
    private String address;

    @Size(max = 255)
    private String city;

    @Size(max = 2)
    private String state;

    @Size(max = 20)
    private String country;

    @Size(max = 10)
    private String zipCode;

    private Boolean isDeleted;

    @Size(max = 100)
    private String addBy;

    private LocalDateTime addDate;

    @Size(max = 100)
    private String lastModBy;

    private LocalDateTime lastModDate;
}
