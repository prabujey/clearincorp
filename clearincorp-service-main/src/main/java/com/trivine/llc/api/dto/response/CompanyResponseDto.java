package com.trivine.llc.api.dto.response;

import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompanyResponseDto {
    @Size(max = 255)
    private String message;

    private Long companyId;
}
