package com.tarzan.maxkb4j.module.model.provider.impl;

import com.tarzan.maxkb4j.module.application.ChatStream;
import com.tarzan.maxkb4j.module.application.TokenStream;
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
import java.util.function.Consumer;

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

    public TokenStream stream0(List<ChatMessage> messages) {
        return new TokenStream() {
            private boolean isRunning = false;
            private Consumer<String> onNextConsumer;
            private Consumer<Response<AiMessage>> onCompleteConsumer;
            private Consumer<Throwable> onErrorConsumer;

            @Override
            public TokenStream onNext(Consumer<String> consumer) {
                this.onNextConsumer = consumer;
                return this;
            }

            @Override
            public TokenStream onComplete(Consumer<Response<AiMessage>> consumer) {
                this.onCompleteConsumer = consumer;
                return this;
            }

            @Override
            public TokenStream onError(Consumer<Throwable> consumer) {
                this.onErrorConsumer = consumer;
                return this;
            }

            @Override
            public void start() {
                if (isRunning) {
                    throw new IllegalStateException("Stream is already running.");
                }
                isRunning = true;
                StreamingResponseHandler<AiMessage> handler = new StreamingResponseHandler<>() {
                    @Override
                    public void onNext(String s) {
                        onNextConsumer.accept(s);
                    }

                    @Override
                    public void onComplete(Response<AiMessage> response) {
                        onCompleteConsumer.accept(response);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        onErrorConsumer.accept(throwable);
                    }
                };
                streamingChatModel.generate(messages, handler);
            }
        };
    }


    public ChatStream stream(List<ChatMessage> messages) {
        final BlockingQueue<AiMessage> messageQueue = new LinkedBlockingQueue<>();
        final AtomicBoolean isCompleted = new AtomicBoolean(false);
        ChatStream chatStream = new ChatStream();
        StreamingResponseHandler<AiMessage> handler = new StreamingResponseHandler<>() {
            @Override
            public void onNext(String s) {
                messageQueue.add(new AiMessage(s));
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                chatStream.setResponse(response);
                messageQueue.add(new AiMessage(""));
                isCompleted.set(true); // 标记流完成
            }

            @Override
            public void onError(Throwable throwable) {
                isCompleted.set(true); // 出错时也标记完成
            }
        };

        streamingChatModel.generate(messages, handler);

        Iterator<AiMessage> iterator = new Iterator<>() {
            @Override
            public boolean hasNext() {
                // 队列不为空或流未完成时继续
                return !messageQueue.isEmpty() || !isCompleted.get();
            }

            @Override
            public AiMessage next() {
                do {
                    AiMessage message = messageQueue.poll();
                    if (message != null) return message;
                } while (!isCompleted.get());
                return null;
            }
        };
        chatStream.setIterator(iterator);
        return chatStream;
    }



    public Iterator<AiMessage> stream1(List<ChatMessage> messages) {
        final BlockingQueue<AiMessage> messageQueue = new LinkedBlockingQueue<>();
        final AtomicBoolean isCompleted = new AtomicBoolean(false);

        StreamingResponseHandler<AiMessage> handler = new StreamingResponseHandler<>() {
            @Override
            public void onNext(String s) {
                messageQueue.add(new AiMessage(s));
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                response.tokenUsage();
                messageQueue.add(new AiMessage(""));
                isCompleted.set(true); // 标记流完成
            }

            @Override
            public void onError(Throwable throwable) {
                isCompleted.set(true); // 出错时也标记完成
            }
        };

        streamingChatModel.generate(messages, handler);

        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                // 队列不为空或流未完成时继续
                return !messageQueue.isEmpty() || !isCompleted.get();
            }

            @Override
            public AiMessage next() {
                do {
                    AiMessage message = messageQueue.poll();
                    if (message != null) return message;
                } while (!isCompleted.get());
                return null;
            }
        };
    }


    public Response<AiMessage> generate(ChatMessage... messages) {
        return chatModel.generate(messages);
    }

    public Response<AiMessage> generate(List<ChatMessage> messages) {
        return chatModel.generate(messages);
    }

}
