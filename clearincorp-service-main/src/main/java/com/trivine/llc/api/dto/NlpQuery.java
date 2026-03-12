package com.trivine.llc.api.dto;

import lombok.*;

import java.util.List;

@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class NlpQuery {
    private String city;
    private String state;
    private String zip;
    private List<String> keywords;
}
