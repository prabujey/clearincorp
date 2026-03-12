package com.trivine.llc.api.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trivine.llc.api.dto.ChatMessageDto;
import com.trivine.llc.api.dto.response.ChatSessionDetailDto;
import com.trivine.llc.api.dto.response.ChatSessionSummaryDto;
import com.trivine.llc.api.entity.ChatSession;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring")
public abstract class ChatSessionMapper {

    @Autowired
    protected ObjectMapper objectMapper;

    // Entity -> Summary DTO
    @Mapping(source = "issue", target = "issue")
    @Mapping(source = "resolved", target = "resolved")
    public abstract ChatSessionSummaryDto toSummaryDto(ChatSession entity);

    // Entity -> Detail DTO (conversation parsed)
    @Mapping(source = "issue", target = "issue")
    @Mapping(source = "resolved", target = "resolved")
    @Mapping(target = "conversation", expression = "java(fromJsonList(entity.getConversation()))")
    public abstract ChatSessionDetailDto toDetailDto(ChatSession entity);

    // Helper methods

    public String toJson(List<ChatMessageDto> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize conversation", e);
        }
    }

    public List<ChatMessageDto> fromJsonList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<ChatMessageDto>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize conversation", e);
        }
    }
}
