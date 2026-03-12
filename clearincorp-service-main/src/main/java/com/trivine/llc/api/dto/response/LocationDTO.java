package com.trivine.llc.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class LocationDTO {

    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String latitude;
    private String longitude;

}
