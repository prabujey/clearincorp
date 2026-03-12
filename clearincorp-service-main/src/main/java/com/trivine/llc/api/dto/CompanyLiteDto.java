package com.trivine.llc.api.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CompanyLiteDto {
    private Long companyId;
    private String companyName;
    private String state;
    private String city;
    private String zipCode;
    private String country;
    private String tradeName;
    private LocalDate companyEffectiveDate;
    private LoginUserLiteDto loginUser;
}
