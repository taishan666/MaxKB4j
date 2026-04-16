package com.maxkb4j.common.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maxkb4j.common.domain.dto.ChatInfo;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class ChatCache {

    // 创建缓存实例，设置最大容量为100，过期时间为10分钟
    private static final Cache<String, ChatInfo> CHAT_CACHE = Caffeine.newBuilder()
            .maximumSize(9999) // 设置缓存的最大容量
            .expireAfterWrite(30, TimeUnit.MINUTES) // 设置写入后30分钟过期
            .build();

    public static void put(String chatId, ChatInfo chatInfo) {
        CHAT_CACHE.put(chatId, chatInfo);
    }

    public static ChatInfo get(String chatId) {
        // 定义一个函数，用于在缓存未命中时生成 ChatInfo 对象
        Function<String, ChatInfo> loader = key -> {
            // 根据键生成 ChatInfo 实例的逻辑
            return null;
        };
        return CHAT_CACHE.get(chatId,loader);
    }
}
