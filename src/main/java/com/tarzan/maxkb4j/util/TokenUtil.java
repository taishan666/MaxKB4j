package com.tarzan.maxkb4j.util;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;

import java.util.List;
import java.util.StringTokenizer;

public class TokenUtil {


    public static int countTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 使用 StringTokenizer 按空格分割文本
        StringTokenizer tokenizer = new StringTokenizer(text, " \t\n\r\f,.!?;:\"'()[]{}<>");
        return tokenizer.countTokens();
    }

    public static int countTokens(List<ChatMessage> messages) {
        StringBuilder textBuilder=new StringBuilder();
        for (ChatMessage chatMessage : messages) {
            if (chatMessage instanceof UserMessage userMessage){
                textBuilder.append(userMessage.singleText());
            }
            if (chatMessage instanceof AiMessage aiMessage){
                textBuilder.append(aiMessage.text());
            }
            if (chatMessage instanceof SystemMessage systemMessage){
                textBuilder.append(systemMessage.text());
            }
        }
        return countTokens(textBuilder.toString());
    }
}
