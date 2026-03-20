package com.maxkb4j.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class MessageUtils {


    /**
     * 生成包含输入和输出的工具消息模版
     *
     * @param icon          图标
     * @param name          工具名称
     * @param input  输入内容（可以是字符串或任意对象）
     * @param output 输出内容（可以是字符串或任意对象）
     * @return 格式化后的工具消息字符串
     */
    public static String buildToolCallRender(String icon, String name,String toolType, String input, String output) {
        try {
            // 构建内容结构
            Map<String, Object> contentMap = new HashMap<>();
            contentMap.put("input", input);
            contentMap.put("output", output);
            Map<String, Object> result = new HashMap<>();
            if (icon!=null&&icon.startsWith(".")){
                icon="/admin"+icon.substring(1);
            }
            result.put("icon", icon);
            result.put("title", name);
            result.put("type", "simple-tool-calls");
            result.put("toolType", toolType);
            result.put("content", contentMap);
            // 序列化为 JSON 字符串
            String jsonContent = new ObjectMapper().writeValueAsString(result);
            return "<tool_calls_render>" + jsonContent + "</tool_calls_render>";
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to generate tool message", e);
        }
    }


    public static String format(List<ChatMessage> chatMemory) {
        return chatMemory.stream().map(MessageUtils::format).filter(Objects::nonNull).collect(Collectors.joining("\n"));
    }

    protected static String format(ChatMessage message) {
        if (message instanceof UserMessage userMessage) {
            return "User: " + userMessage.singleText();
        } else if (message instanceof AiMessage aiMessage) {
            return aiMessage.hasToolExecutionRequests() ? null : "AI: " + aiMessage.text();
        } else {
            return null;
        }
    }


}
