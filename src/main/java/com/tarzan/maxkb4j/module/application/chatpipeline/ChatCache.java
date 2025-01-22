package com.tarzan.maxkb4j.module.application.chatpipeline;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatCache {
    private final static Map<String,ChatInfo> CHAT_CACHE = new ConcurrentHashMap<>();

    public static void put(String chatId, ChatInfo chatInfo) {
        CHAT_CACHE.put(chatId, chatInfo);
    }
    public static ChatInfo get(String chatId) {
        return CHAT_CACHE.get(chatId);
    }
}
