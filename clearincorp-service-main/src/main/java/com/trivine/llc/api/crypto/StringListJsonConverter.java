package com.trivine.llc.api.crypto;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.List;

@Converter
public class StringListJsonConverter implements AttributeConverter<List<String>, String> {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<String> list) {
        try {
            return (list == null || list.isEmpty()) ? "[]"
                    : MAPPER.writeValueAsString(list);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write attachment keys JSON", e);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String json) {
        try {
            if (json == null || json.isBlank()) return Collections.emptyList();
            return MAPPER.readValue(json, TYPE);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read attachment keys JSON", e);
        }
    }
}
