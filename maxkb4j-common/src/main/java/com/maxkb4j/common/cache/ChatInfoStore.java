package com.maxkb4j.common.cache;

import com.maxkb4j.common.domain.dto.ChatInfo;

/**
 * 聊天会话缓存存储抽象。
 * <p>默认实现为 {@link ChatCache} 门面内置的 Caffeine 缓存；Spring 启动后由
 * application 模块的 {@code ChatInfoCacheService} 注册 Bean 实现替换。</p>
 */
public interface ChatInfoStore {

    void put(String chatId, ChatInfo chatInfo);

    ChatInfo get(String chatId);

    void remove(String chatId);
}