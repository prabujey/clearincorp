package com.trivine.llc.api.dto;

import lombok.*;
import jakarta.validation.constraints.Size;


import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuffixMasterDTO {
    private Long suffixId;
    @Size(max = 255, message = "Suffix must not exceed 255 characters")
    private String suffix;

    private LocalDateTime createdOn;
    private Boolean isActive;
}
