package com.trivine.llc.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessDetailsDto {
    private Long companyId;

    @NotBlank
    @Size(max = 255)
    private String businessDescription;

    @NotBlank
    @Size(max = 100)
    private String tradeName;

    private String principalActivity;
}
