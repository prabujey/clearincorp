package com.trivine.llc.api.service;

import com.trivine.llc.api.dto.ChatMessageDto;
import com.trivine.llc.api.dto.request.ChatReviewCreateRequest;
import com.trivine.llc.api.dto.request.ChatSessionFilter;
import com.trivine.llc.api.dto.response.ChatSessionDetailDto;
import com.trivine.llc.api.dto.response.ChatSessionSummaryDto;
import com.trivine.llc.api.entity.ChatSession;
import com.trivine.llc.api.mapper.ChatSessionMapper;
import com.trivine.llc.api.repository.ChatSessionRepository;
import com.trivine.llc.api.repository.specifications.ChatSessionSpecifications;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatSessionMapper mapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public void createFromReview(ChatReviewCreateRequest req) {
        if (req.getSessionId() == null || req.getSessionId().isBlank()) {
            throw new IllegalArgumentException("sessionId is required");
        }
        if (req.getRating() == null) {
            throw new IllegalArgumentException("rating is required");
        }
        if (req.getConversation() == null || req.getConversation().isEmpty()) {
            throw new IllegalArgumentException("conversation is required");
        }

        // One review per session
        if (chatSessionRepository.existsBySessionId(req.getSessionId())) {
            throw new IllegalStateException("Review already submitted for this session");
        }

        String conversationJson = serializeConversation(req.getConversation());
        boolean isIssue = req.getRating() != null && req.getRating() <= 3;

        ChatSession entity = ChatSession.builder()
                .sessionId(req.getSessionId())
                .userId(req.getUserId())
                .userEmail(req.getUserEmail())
                .conversation(conversationJson)
                .rating(req.getRating())
                .reviewComment(req.getReviewComment())
                .issue(isIssue)
                .resolved(false)
                .build();

        chatSessionRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public ChatSessionDetailDto getBySessionId(String sessionId) {
        ChatSession entity = chatSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        return mapper.toDetailDto(entity);
    }

    @Transactional(readOnly = true)
    public Page<ChatSessionSummaryDto> search(ChatSessionFilter filter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var spec = ChatSessionSpecifications.withFilter(filter);
        return chatSessionRepository.findAll(spec, pageable)
                .map(mapper::toSummaryDto);
    }

    @Transactional
    public void markResolved(String sessionId, boolean resolved) {
        ChatSession entity = chatSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        entity.setResolved(resolved);
        chatSessionRepository.save(entity);
    }

    private String serializeConversation(List<ChatMessageDto> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize conversation", e);
        }
    }
}
