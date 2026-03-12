package com.trivine.llc.api.ws;

import lombok.Value;

@Value
public class GenericWsEvent {
    String destination;
    Object payload;
}
