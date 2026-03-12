package com.trivine.llc.api.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@Getter
@Setter
public class TitleDto {
    private Long titleId;
    private Long topicId;
    private String title;
    private String descriptionMd;
    private Instant createdAt;
    private Long createdById;
    private String createdByName;
    private List<PostDto> posts;
}
