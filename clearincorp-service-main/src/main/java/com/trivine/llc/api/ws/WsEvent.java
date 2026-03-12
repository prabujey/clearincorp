package com.trivine.llc.api.ws;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class WsEvent<T> {
    private String type;
    private T payload;
    private Long refId;
}
