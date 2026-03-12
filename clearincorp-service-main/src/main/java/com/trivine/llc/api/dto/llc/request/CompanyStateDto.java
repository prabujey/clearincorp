package com.trivine.llc.api.dto.llc.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class CompanyStateDto {

    @NotBlank
    @Size(max = 50)
    private String state;
    private Long loginUserId;
    private Long companyId;

}
