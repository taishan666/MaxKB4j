package com.tarzan.maxkb4j.module.rag;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;

public class MyCompressingQueryTransformer extends CompressingQueryTransformer {
    public MyCompressingQueryTransformer(ChatLanguageModel chatLanguageModel) {
        super(chatLanguageModel);
    }

    @Override
    protected String format(ChatMessage message) {
        if (message instanceof SystemMessage msg) {
            return "System: " + msg.text();
        }else if (message instanceof UserMessage msg) {
            return "User: " + msg.singleText();
        } else if (message instanceof AiMessage aiMessage) {
            return aiMessage.hasToolExecutionRequests() ? null : "AI: " + aiMessage.text();
        } else {
            return null;
        }
    }
}
