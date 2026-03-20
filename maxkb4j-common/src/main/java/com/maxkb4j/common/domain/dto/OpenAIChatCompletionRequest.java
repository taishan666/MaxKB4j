package com.maxkb4j.common.domain.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.List;

/**
 * OpenAI Chat Completion API 请求格式
 * https://platform.openai.com/docs/api-reference/chat/create
 */
@Data
public class OpenAIChatCompletionRequest {

    /**
     * 模型ID
     */
    private String model;

    /**
     * 消息列表
     */
    private List<OpenAIMessage> messages;

    /**
     * 温度参数 (0-2)
     */
    private Double temperature;

    /**
     * Top-p 采样参数
     */
    @JSONField(name = "top_p")
    private Double topP;

    /**
     * 最大生成 token 数
     */
    @JSONField(name = "max_tokens")
    private Integer maxTokens;

    /**
     * 是否流式输出
     */
    private Boolean stream;

    /**
     * 停止词
     */
    private List<String> stop;

    /**
     * 频率惩罚 (-2.0 to 2.0)
     */
    @JSONField(name = "frequency_penalty")
    private Double frequencyPenalty;

    /**
     * 存在惩罚 (-2.0 to 2.0)
     */
    @JSONField(name = "presence_penalty")
    private Double presencePenalty;

    /**
     * 用户标识
     */
    private String user;

    /**
     * 获取最后一条用户消息
     */
    public String getLastUserMessage() {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            OpenAIMessage msg = messages.get(i);
            if ("user".equals(msg.getRole())) {
                return msg.getContent();
            }
        }
        return null;
    }

    /**
     * 获取系统消息
     */
    public String getSystemMessage() {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        for (OpenAIMessage msg : messages) {
            if ("system".equals(msg.getRole())) {
                return msg.getContent();
            }
        }
        return null;
    }
}