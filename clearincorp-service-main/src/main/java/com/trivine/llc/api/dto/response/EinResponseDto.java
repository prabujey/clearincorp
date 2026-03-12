package com.trivine.llc.api.dto.response;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EinResponseDto {
    private Long companyId;
    private String companyName;
    private String state;
    private Boolean IsExpeditedServiceSelected;
    private LocalDate date;
}