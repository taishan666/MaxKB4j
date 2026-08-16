package com.maxkb4j.common.cache;

import com.maxkb4j.common.domain.dto.ChatInfo;

/**
 * 聊天会话缓存存储抽象。
 *
 * <p>默认实现为 {@link ChatInfoCacheService}（Spring 管理的 Caffeine 本地缓存），
 * chat / workflow / application 各模块统一注入该 Bean 使用。</p>
 */
public interface ChatInfoStore {

    void put(String chatId, ChatInfo chatInfo);

    ChatInfo get(String chatId);

    void remove(String chatId);
}