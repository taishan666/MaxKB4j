package com.tarzan.maxkb4j.module.application;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;

public class MyCompressingQueryTransformer extends CompressingQueryTransformer {
    public MyCompressingQueryTransformer(ChatLanguageModel chatLanguageModel) {
        super(chatLanguageModel);
    }

    public MyCompressingQueryTransformer(ChatLanguageModel chatLanguageModel, PromptTemplate promptTemplate) {
        super(chatLanguageModel, promptTemplate);
    }

    @Override
    protected String format(ChatMessage message) {
        if (message instanceof SystemMessage msg) {
            return "System: " + msg.text();
        }else if (message instanceof UserMessage msg) {
            return "User: " + msg.singleText();
        } else if (message instanceof AiMessage) {
            AiMessage aiMessage = (AiMessage)message;
            return aiMessage.hasToolExecutionRequests() ? null : "AI: " + aiMessage.text();
        } else {
            return null;
        }
    }
}
