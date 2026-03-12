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
public class StatusResponseDto {
    private Long companyId;
    private String companyName;
    private String status;
    private String state;
    private Boolean IsExpeditedServiceSelected;
    private Boolean IsEinSelected;
    private LocalDate date;
}
