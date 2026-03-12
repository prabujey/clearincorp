package com.trivine.llc.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditClickResponseDto {
    private Long id;
    private Long businessId;
    private Long ownerId;
    private String sessionId;
    private Instant occurredAt;
    private boolean deduped;
}
