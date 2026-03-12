package com.trivine.llc.api.controller.admin;

import com.trivine.llc.api.dto.request.ChatSessionFilter;
import com.trivine.llc.api.dto.response.ChatSessionDetailDto;
import com.trivine.llc.api.dto.response.ChatSessionSummaryDto;
import com.trivine.llc.api.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/chat")
@RequiredArgsConstructor
public class AdminChatController {

    private final ChatSessionService chatSessionService;

    @GetMapping("/reviews")
    public ResponseEntity<Page<ChatSessionSummaryDto>> list(
            @RequestParam(required = false) Byte minRating,
            @RequestParam(required = false) Byte maxRating,
            @RequestParam(required = false) Boolean issue,
            @RequestParam(required = false) Boolean resolved,
            @RequestParam(required = false) String userEmail,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        ChatSessionFilter filter = ChatSessionFilter.builder()
                .minRating(minRating)
                .maxRating(maxRating)
                .issue(issue)
                .resolved(resolved)
                .userEmail(userEmail)
                .build();

        Page<ChatSessionSummaryDto> result = chatSessionService.search(filter, page, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/reviews/{sessionId}")
    public ResponseEntity<ChatSessionDetailDto> get(@PathVariable String sessionId) {
        return ResponseEntity.ok(chatSessionService.getBySessionId(sessionId));
    }

    @PatchMapping("/reviews/{sessionId}/resolve")
    public ResponseEntity<?> resolve(
            @PathVariable String sessionId,
            @RequestParam boolean resolved
    ) {
        chatSessionService.markResolved(sessionId, resolved);
        return ResponseEntity.ok().build();
    }
}
