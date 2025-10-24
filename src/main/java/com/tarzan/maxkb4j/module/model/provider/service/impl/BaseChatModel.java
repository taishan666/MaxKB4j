package com.tarzan.maxkb4j.module.model.provider.service.impl;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.DisabledChatModel;
import dev.langchain4j.model.chat.DisabledStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.Getter;

@Getter
public class BaseChatModel {

    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;

    public BaseChatModel() {
        this.streamingChatModel = new DisabledStreamingChatModel();
        this.chatModel = new DisabledChatModel();
    }

    public BaseChatModel(StreamingChatModel streamingChatModel, ChatModel chatModel) {
        this.streamingChatModel = streamingChatModel;
        this.chatModel = chatModel;
    }

    public ChatResponse generate(ChatMessage... messages) {
        return chatModel.chat(messages);
    }


}
