package com.trivine.llc.api.ws;

public final class WsTopics {
    private WsTopics() {}
    public static String title(long titleId) { return "/topic/title." + titleId; }
    public static String post(long postId)   { return "/topic/post." + postId; }
}
