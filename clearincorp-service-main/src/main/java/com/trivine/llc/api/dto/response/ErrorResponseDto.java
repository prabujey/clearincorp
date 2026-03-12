package com.trivine.llc.api.dto.response;


import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDto {
    private String message;
    private String details;
    private LocalDateTime timeStamp;
    public ErrorResponseDto(String message, String details) {
        this.message = message;
        this.details = details;
        this.timeStamp = LocalDateTime.now();
    }


}
