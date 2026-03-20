package com.maxkb4j.common.domain.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAI Chat Completion Choice 格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAIChoice {

    /**
     * 选择索引
     */
    private Integer index;

    /**
     * 消息 (非流式响应)
     */
    private OpenAIMessage message;

    /**
     * 增量消息 (流式响应)
     */
    private OpenAIMessage delta;

    /**
     * 结束原因: stop, length, content_filter, null
     */
    @JSONField(name = "finish_reason")
    private String finishReason;

    /**
     * 创建助手消息选择 (非流式)
     */
    public static OpenAIChoice createAssistantChoice(Integer index, String content) {
        return OpenAIChoice.builder()
                .index(index)
                .message(OpenAIMessage.assistant(content))
                .finishReason("stop")
                .build();
    }

    /**
     * 创建增量消息选择 (流式)
     */
    public static OpenAIChoice createDeltaChoice(Integer index, String deltaContent, String finishReason) {
        return OpenAIChoice.builder()
                .index(index)
                .delta(deltaContent != null ? OpenAIMessage.builder().content(deltaContent).build() : null)
                .finishReason(finishReason)
                .build();
    }

    /**
     * 创建增量消息选择 (流式，带角色)
     */
    public static OpenAIChoice createDeltaChoiceWithRole(Integer index, String role, String deltaContent, String finishReason) {
        return OpenAIChoice.builder()
                .index(index)
                .delta(OpenAIMessage.builder().role(role).content(deltaContent).build())
                .finishReason(finishReason)
                .build();
    }
}