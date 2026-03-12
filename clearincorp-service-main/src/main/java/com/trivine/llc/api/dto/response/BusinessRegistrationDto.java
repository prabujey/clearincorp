package com.trivine.llc.api.dto.response;


import com.trivine.llc.api.dto.BusinessDto;
import com.trivine.llc.api.dto.BusinessOwnerDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BusinessRegistrationDto {
    private BusinessOwnerDto owner;
    private BusinessDto business;
}
