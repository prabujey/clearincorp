package com.trivine.llc.api.dto;

public record TitleCheckResponse(
        Long titleId,          // null if not found
        Long topicId,          // null if not found
        String titleNormalized,
        boolean exists,        // true if a duplicate row exists
        boolean available      // convenience: !exists
) {}
