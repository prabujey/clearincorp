package com.trivine.llc.api.dto;

import java.io.Serializable;


public record BusinessImageDto(
        int imageId,
        String key,
        long size,
        String lastModified,
        String eTag
) implements Serializable {}
