package com.trivine.llc.api.ws;

import lombok.Value;

@Value
public class PostCreatedEvent {
    Long postId;
    Long titleId;
    Object postDto;
}
