package com.maxkb4j.common.domain.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAI Token 使用量格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAIUsage {

    /**
     * 提示词 token 数
     */
    @JSONField(name = "prompt_tokens")
    private Integer promptTokens;

    /**
     * 完成 token 数
     */
    @JSONField(name = "completion_tokens")
    private Integer completionTokens;

    /**
     * 总 token 数
     */
    @JSONField(name = "total_tokens")
    private Integer totalTokens;

    /**
     * 创建使用量对象
     */
    public static OpenAIUsage create(Integer promptTokens, Integer completionTokens) {
        int total = (promptTokens != null ? promptTokens : 0) + (completionTokens != null ? completionTokens : 0);
        return OpenAIUsage.builder()
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(total)
                .build();
    }
}