package com.trivine.llc.api.dto.request;

import com.trivine.llc.api.dto.ChatMessageDto;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatReviewCreateRequest {

    private String sessionId;
    private Long userId;
    private String userEmail;

    private Byte rating;                // 1–5
    private String reviewComment;

    private List<ChatMessageDto> conversation;  // full history for this session
}
