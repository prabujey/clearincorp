package com.trivine.llc.api.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaceDTO {

    @SerializedName("postal_code")
    @JsonProperty("postal_code")
    private String postalCode;

    @JsonProperty("place name")
    private String placeName;

    @JsonProperty("state")
    private String state;

    @JsonProperty("country")
    private String country;

    @JsonProperty("state abbreviation")
    private String stateAbbreviation;

    @JsonProperty("latitude")
    private String latitude;

    @JsonProperty("longitude")
    private String longitude;


}
