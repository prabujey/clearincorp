package com.trivine.llc.api.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class DocumentResponseDto {
    private String CompanyName;
    private String jurisdiction;
    private String type;
    private Long companyId;
    private boolean isViewed;
    private LocalDateTime uploadedOn;
}
