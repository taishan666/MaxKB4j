package com.tarzan.maxkb4j.core.tool;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.tool.ToolExecution;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MessageTools {

    private static final String tool_message_template = """
            <details>
                <summary>
                    <strong>Called Tool: <em>%s</em></strong>
                </summary>
            
            %s
            
            </details>
            
            """;

    private static final String tool_message_json_template = """
            ```json
            %s
            ```
            """;

    public static String getToolMessage(String toolName, String toolMessage) {
        String toolMessageJson= String.format(tool_message_json_template,  toolMessage);
        return String.format(tool_message_template, toolName, toolMessageJson);
    }

    public static String getToolMessage(ToolExecution toolExecute) {
        return getToolMessage(toolExecute.request().name(), toolExecute.result());
    }

    public static String format(List<ChatMessage> chatMemory) {
        return chatMemory.stream().map(MessageTools::format).filter(Objects::nonNull).collect(Collectors.joining("\n"));
    }

    protected  static String format(ChatMessage message) {
        if (message instanceof UserMessage userMessage) {
            return "User: " + userMessage.singleText();
        } else if (message instanceof AiMessage aiMessage) {
            return aiMessage.hasToolExecutionRequests() ? null : "AI: " + aiMessage.text();
        } else {
            return null;
        }
    }


}
