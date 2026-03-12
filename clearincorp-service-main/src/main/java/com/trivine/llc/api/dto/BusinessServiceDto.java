package com.trivine.llc.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BusinessServiceDto {
    private Long serviceId;
    private String serviceName;
    private String description;
}
