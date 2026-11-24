package com.maxkb4j.common.cache;

import com.maxkb4j.common.domain.dto.ChatInfo;

/**
 * 聊天会话缓存存储抽象。
 *
 * <p>默认实现为 {@link CaffeineChatInfoStore}（Caffeine 本地缓存），
 * chat / workflow / application 各模块通过构造函数注入统一使用；
 * 需要多实现时用 {@code @Primary} / {@code @Qualifier} 选择。</p>
 */

public interface ChatInfoStore {

    void put(String chatId, ChatInfo chatInfo);

    ChatInfo get(String chatId);

    void remove(String chatId);
}