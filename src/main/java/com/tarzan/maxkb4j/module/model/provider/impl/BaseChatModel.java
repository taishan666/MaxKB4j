package com.tarzan.maxkb4j.module.model.provider.impl;

import com.tarzan.maxkb4j.module.application.ChatStream;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.DisabledChatLanguageModel;
import dev.langchain4j.model.chat.DisabledStreamingChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

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

    public void stream(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
        streamingChatModel.generate(messages, handler);
    }


    public ChatStream stream(List<ChatMessage> messages) {
        long startTime = System.currentTimeMillis();
        final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
        final AtomicBoolean isCompleted = new AtomicBoolean(false);
        ChatStream chatStream = new ChatStream();
        StreamingResponseHandler<AiMessage> handler = new StreamingResponseHandler<>() {
            @Override
            public void onNext(String token) {
                messageQueue.add(token);
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                System.out.println("耗时 onComplete "+(System.currentTimeMillis()-startTime)+" ms");
                // 调用 ChatStream 的回调函数
                chatStream.invokeOnComplete(response);
                messageQueue.add("");
                isCompleted.set(true); // 标记流完成
            }

            @Override
            public void onError(Throwable throwable) {
                isCompleted.set(true); // 出错时也标记完成
            }
        };

        streamingChatModel.generate(messages, handler);

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
    }


    public Response<AiMessage> generate(ChatMessage... messages) {
        return chatModel.generate(messages);
    }

    public Response<AiMessage> generate(List<ChatMessage> messages) {
        return chatModel.generate(messages);
    }

}
