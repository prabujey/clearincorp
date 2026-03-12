package com.trivine.llc.api.dto.llc.request;

import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class CompanyRequestDto {

    @Size(max = 150)
    private String companyName;

    @Size(max = 50)
    private String llcSuffix;

    @Size(max = 50)
    private String state;

    private Long loginUserId;

    private Long companyId;

}
