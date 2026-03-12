package com.trivine.llc.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReplyCreateRequestDto {
    @NotNull private Long postId;
    private Long parentReplyId;
    @NotBlank private String contentMd;
}
