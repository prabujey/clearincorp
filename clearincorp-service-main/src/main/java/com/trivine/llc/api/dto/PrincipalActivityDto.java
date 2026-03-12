package com.trivine.llc.api.dto;

import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PrincipalActivityDto {
    private Long id;
    private String value;
    private List<String> subActivities;
}
