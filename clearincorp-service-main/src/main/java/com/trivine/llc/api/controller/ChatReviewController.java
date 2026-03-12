package com.trivine.llc.api.controller;

import com.trivine.llc.api.dto.request.ChatReviewCreateRequest;
import com.trivine.llc.api.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatReviewController {

    private final ChatSessionService chatSessionService;

    @PostMapping("/reviews")
    public ResponseEntity<?> createReview(@RequestBody ChatReviewCreateRequest request) {
        chatSessionService.createFromReview(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/reviews/{sessionId}")
    public ResponseEntity<?> getReview(@PathVariable String sessionId) {
        var dto = chatSessionService.getBySessionId(sessionId);
        return ResponseEntity.ok(dto);
    }
}

