package com.trivine.llc.api.dto;


import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyFilterDto {
    private String principalActivity;     // exact or contains (we'll do contains/ILIKE)
    private List<String> states;          // e.g. ["CA","NY"]
    private LocalDate effectiveFrom;      // companyEffectiveDate >=
    private LocalDate effectiveTo;        // companyEffectiveDate <=
    private String search;                // search in name/trade/city/activity
}
