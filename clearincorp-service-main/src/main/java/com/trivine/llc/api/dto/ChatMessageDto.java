package com.trivine.llc.api.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private String sender;      // "user" / "bot"
    private String text;
    private String timestamp;   // ISO string from frontend
}
