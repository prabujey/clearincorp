package com.trivine.llc.api.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;

@Converter
public class EncryptResultConverter implements AttributeConverter<Object, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Object encryptResult) {
        if (encryptResult == null) {
            return null;
        }
        try {
            // Cast the Object to EncryptResult and convert to JSON
            EncryptResult result = (EncryptResult) encryptResult;
            return objectMapper.writeValueAsString(result); // Convert object to JSON
        } catch (IOException e) {
            throw new RuntimeException("Error converting EncryptResult to JSON", e);
        }
    }

    @Override
    public Object convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            // Convert JSON string back to EncryptResult object
            return objectMapper.readValue(dbData, EncryptResult.class); // Convert JSON back to object
        } catch (IOException e) {
            throw new RuntimeException("Error converting JSON to EncryptResult", e);
        }
    }
}
