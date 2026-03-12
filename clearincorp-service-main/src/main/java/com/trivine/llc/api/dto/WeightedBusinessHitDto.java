package com.trivine.llc.api.dto;


import lombok.*;

import java.util.Map;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class WeightedBusinessHitDto {
    private BusinessDto business;
    private double score;
    private Map<String, Double> wordWeights;
    private Map<String, Double> fieldWeights;
}
