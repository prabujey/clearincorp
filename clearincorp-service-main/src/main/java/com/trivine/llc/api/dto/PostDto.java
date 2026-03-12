package com.trivine.llc.api.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;


@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PostDto {
    private Long postId;

    private Long loginUserId;
    private String loginUserName;

    private Long topicId;
    private String topicName;

    private Long titleId;
    private String title;
    private String content;

    private boolean pinned;
    private Long pinnedById;

    private int likesCount;
    private int viewsCount;
    private int replyCount;

    private boolean deleted;
    private Instant createdAt;
    private Instant editedAt;
    private Instant lastActivityAt;
    private List<ReplyDto> replies;
}
