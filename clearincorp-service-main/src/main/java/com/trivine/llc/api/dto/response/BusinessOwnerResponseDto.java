package com.trivine.llc.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessOwnerResponseDto {
    private String firstName;
    private String lastName;
    private String personalEmail;
    private String ContactNumber;
    private Long loginUserId;
    private Long OwnerId;
}