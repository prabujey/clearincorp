package com.trivine.llc.api.dto;

import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplyDto {

    private Long replyId;
    private Long postId;
    private Long parentReplyId;
    private Long loginUserId;
    private String loginUserName;
    private String content;
    private short depth;
    private int likesCount;
    private boolean deleted;
    private Instant createdAt;
    private Instant editedAt;
    @Builder.Default
    private List<ReplyDto> children = new ArrayList<>();
}
