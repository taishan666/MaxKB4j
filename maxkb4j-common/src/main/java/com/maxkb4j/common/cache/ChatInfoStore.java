package com.maxkb4j.common.cache;

import com.maxkb4j.common.domain.dto.ChatInfo;

/**
 * 聊天会话缓存存储抽象。
 *
 * <p>默认实现为 {@link ChatCache} 门面内置的 Caffeine 本地缓存，
 * chat / workflow / application 各模块通过静态 API 统一使用。</p>
 */

public interface ChatInfoStore {

    void put(String chatId, ChatInfo chatInfo);

    ChatInfo get(String chatId);

    void remove(String chatId);
}