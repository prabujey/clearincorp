package com.trivine.llc.api.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class VendorPageDto {
    private LoginUserDto loginUserDto;
    private UserCompanyDto userCompanyDto;
}
