package com.maxkb4j.common.domain.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OpenAI Chat Completion API 响应格式
 * https://platform.openai.com/docs/api-reference/chat/object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAIChatCompletionResponse {

    /**
     * 响应唯一标识
     */
    private String id;

    /**
     * 对象类型: "chat.completion" 或 "chat.completion.chunk"
     */
    private String object;

    /**
     * 创建时间戳 (Unix 时间戳)
     */
    private Long created;

    /**
     * 使用的模型
     */
    private String model;

    /**
     * 选择列表
     */
    private List<OpenAIChoice> choices;

    /**
     * Token 使用量 (可选，流式响应中通常不返回)
     */
    private OpenAIUsage usage;

    /**
     * 系统指纹 (可选)
     */
    @JSONField(name = "system_fingerprint")
    private String systemFingerprint;

    /**
     * 创建非流式响应
     */
    public static OpenAIChatCompletionResponse createCompletion(String id, String model, String content, Integer promptTokens, Integer completionTokens) {
        return OpenAIChatCompletionResponse.builder()
                .id(id)
                .object("chat.completion")
                .created(System.currentTimeMillis() / 1000)
                .model(model)
                .choices(List.of(OpenAIChoice.createAssistantChoice(0, content)))
                .usage(OpenAIUsage.create(promptTokens, completionTokens))
                .build();
    }

    /**
     * 创建流式响应块
     */
    public static OpenAIChatCompletionResponse createChunk(String id, String model, Integer index, String deltaContent, String finishReason) {
        return OpenAIChatCompletionResponse.builder()
                .id(id)
                .object("chat.completion.chunk")
                .created(System.currentTimeMillis() / 1000)
                .model(model)
                .choices(List.of(OpenAIChoice.createDeltaChoice(index, deltaContent, finishReason)))
                .build();
    }

    /**
     * 创建流式响应块 (带角色)
     */
    public static OpenAIChatCompletionResponse createChunkWithRole(String id, String model, Integer index, String role, String deltaContent, String finishReason) {
        return OpenAIChatCompletionResponse.builder()
                .id(id)
                .object("chat.completion.chunk")
                .created(System.currentTimeMillis() / 1000)
                .model(model)
                .choices(List.of(OpenAIChoice.createDeltaChoiceWithRole(index, role, deltaContent, finishReason)))
                .build();
    }
}