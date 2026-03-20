package com.maxkb4j.common.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAI Chat Message 格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAIMessage {

    /**
     * 角色: system, user, assistant, tool
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 名称 (可选)
     */
    private String name;

    /**
     * 工具调用 (可选)
     */
    // private List<OpenAIToolCall> toolCalls;

    /**
     * 创建系统消息
     */
    public static OpenAIMessage system(String content) {
        return OpenAIMessage.builder().role("system").content(content).build();
    }

    /**
     * 创建用户消息
     */
    public static OpenAIMessage user(String content) {
        return OpenAIMessage.builder().role("user").content(content).build();
    }

    /**
     * 创建助手消息
     */
    public static OpenAIMessage assistant(String content) {
        return OpenAIMessage.builder().role("assistant").content(content).build();
    }
}