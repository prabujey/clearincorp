package com.trivine.llc.api.controller;

import com.trivine.llc.api.dto.chatbot.ChatRequestDto;
import com.trivine.llc.api.service.ChatbotService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.Map;

@RestController
@RequestMapping("/chatbot")
@Validated
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/chat")
    public Mono<ResponseEntity<Map<String, String>>> chat(@Valid @RequestBody ChatRequestDto requestDto) {
        // Sanitize message input before processing
        String sanitizedMessage = requestDto.getMessage() != null
                ? requestDto.getMessage().trim()
                : "";

        if (sanitizedMessage.isEmpty() || sanitizedMessage.length() > 2000) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("error", "Message must be between 1 and 2000 characters")));
        }

        return this.chatbotService.getRagResponse(sanitizedMessage)
                .map(response -> ResponseEntity.ok(Map.of("reply", response)));
    }
}