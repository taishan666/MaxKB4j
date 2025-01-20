package com.tarzan.maxkb4j.module.application.chatpipeline;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatCache {
    private final static Map<UUID,ChatInfo> CHAT_CACHE = new ConcurrentHashMap<>();

    public static void put(UUID uuid, ChatInfo chatInfo) {
        CHAT_CACHE.put(uuid, chatInfo);
    }
    public static ChatInfo get(UUID uuid) {
        return CHAT_CACHE.get(uuid);
    }
}
