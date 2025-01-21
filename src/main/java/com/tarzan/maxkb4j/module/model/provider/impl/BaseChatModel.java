package com.tarzan.maxkb4j.module.model.provider.impl;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.DisabledChatLanguageModel;
import dev.langchain4j.model.chat.DisabledStreamingChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;

import java.util.List;

public class BaseChatModel  {

    private final ChatLanguageModel chatModel;
    private final StreamingChatLanguageModel streamingChatModel;

    public BaseChatModel() {
        this.streamingChatModel = new DisabledStreamingChatLanguageModel();
        this.chatModel = new DisabledChatLanguageModel();
    }

    public BaseChatModel(StreamingChatLanguageModel streamingChatModel, ChatLanguageModel chatModel){
        this.streamingChatModel = streamingChatModel;
        this.chatModel = chatModel;
    }

    public void stream(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler){
        streamingChatModel.generate(messages, handler);
    }

    public Response<AiMessage> generate(ChatMessage... messages){
        return chatModel.generate(messages);
    }

    public Response<AiMessage> generate(List<ChatMessage> messages){
        return chatModel.generate(messages);
    }

}
