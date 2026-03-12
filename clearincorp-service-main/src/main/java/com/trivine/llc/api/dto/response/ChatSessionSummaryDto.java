package com.trivine.llc.api.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionSummaryDto {
    private String sessionId;
    private Long userId;
    private String userEmail;
    private Byte rating;
    private String reviewComment;
    private boolean issue;
    private boolean resolved;
    private LocalDateTime createdAt;
}
