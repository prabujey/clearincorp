package com.trivine.llc.api.dto.request;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionFilter {
    private Byte minRating;
    private Byte maxRating;
    private Boolean issue;
    private Boolean resolved;
    private String userEmail;
    private LocalDateTime from;
    private LocalDateTime to;
}
