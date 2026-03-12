
package com.trivine.llc.api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditClickRequestDto {
    private Long businessId;
    private Long ownerId;
    private String sessionId;
}
