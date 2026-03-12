package com.trivine.llc.api.dto.response;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.trivine.llc.api.dto.PlaceDTO;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZippopotZipResponseDTO {

    @JsonProperty("places")
    private List<PlaceDTO> places;

}
