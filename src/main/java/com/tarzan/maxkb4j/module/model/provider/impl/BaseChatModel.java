package com.tarzan.maxkb4j.module.model.provider.impl;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.DisabledChatLanguageModel;
import dev.langchain4j.model.chat.DisabledStreamingChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.Getter;

import java.util.List;

@Getter
public class BaseChatModel {

    private final ChatLanguageModel chatModel;
    private final StreamingChatLanguageModel streamingChatModel;

    public BaseChatModel() {
        this.streamingChatModel = new DisabledStreamingChatLanguageModel();
        this.chatModel = new DisabledChatLanguageModel();
    }

    public BaseChatModel(StreamingChatLanguageModel streamingChatModel, ChatLanguageModel chatModel) {
        this.streamingChatModel = streamingChatModel;
        this.chatModel = chatModel;
    }

/*    public void stream(List<ChatMessage> messages, StreamingChatResponseHandler handler) {
        streamingChatModel.chat(messages, handler);
    }

    public ChatStream stream(List<ChatMessage> messages) {
        final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
        final AtomicBoolean isCompleted = new AtomicBoolean(false);
        ChatStream chatStream = new ChatStream();
        StreamingChatResponseHandler handler = new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String token) {
                messageQueue.add(token);
            }

            @Override
            public void onCompleteResponse(ChatResponse chatResponse) {
                // 调用 ChatStream 的回调函数
                chatStream.onComplete(chatResponse);
                messageQueue.add("");
                isCompleted.set(true); // 标记流完成
            }

            @Override
            public void onError(Throwable throwable) {
                messageQueue.add(throwable.getMessage());
                isCompleted.set(true); // 出错时也标记完成
            }
        };
        streamingChatModel.chat(messages, handler);
        Iterator<String> iterator = new Iterator<>() {
            @Override
            public boolean hasNext() {
                // 队列不为空或流未完成时继续
                return !messageQueue.isEmpty() || !isCompleted.get();
            }

            @Override
            public String next() {
                do {
                    String message = messageQueue.poll();
                    if (message != null) return message;
                } while (!isCompleted.get());
                return null;
            }
        };
        chatStream.setIterator(iterator);
        return chatStream;
    }*/


    public ChatResponse generate(ChatMessage... messages) {
        return chatModel.chat(messages);
    }

    public ChatResponse generate(List<ChatMessage> messages) {
        return chatModel.chat(messages);
    }

}
