package com.trivine.llc.api.dto.response;

import com.trivine.llc.api.dto.ChatMessageDto;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionDetailDto {
    private String sessionId;
    private Long userId;
    private String userEmail;
    private Byte rating;
    private String reviewComment;
    private boolean issue;
    private boolean resolved;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ChatMessageDto> conversation;
}
