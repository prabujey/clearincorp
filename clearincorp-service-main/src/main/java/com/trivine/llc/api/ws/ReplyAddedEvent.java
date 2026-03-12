package com.trivine.llc.api.ws;

import lombok.Value;

@Value
public class ReplyAddedEvent {
    Long postId;
    Long titleId;
    Object replyDto; // ReplyDto
}
