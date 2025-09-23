package com.tarzan.maxkb4j.core.langchain4j;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.service.memory.ChatMemoryService;

import java.util.List;

import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

public class AppChatMemory implements ChatMemory {
    private final Object id;
    private final List<ChatMessage> messages;

    private AppChatMemory(AppChatMemory.Builder builder) {
        this.id = ensureNotNull(builder.id, "id");
        this.messages=builder.messages;
    }

    @Override
    public Object id() {
        return id;
    }

    @Override
    public void add(ChatMessage message) {
        if (message instanceof SystemMessage){
            messages.add(0,message);
        }
        if (message instanceof UserMessage){
            messages.add(message);
        }
    }

    @Override
    public List<ChatMessage> messages() {
        return messages;
    }


    @Override
    public void clear() {
    }

    public static AppChatMemory.Builder builder() {
        return new AppChatMemory.Builder();
    }

    public static class Builder {

        private Object id = ChatMemoryService.DEFAULT;
        private List<ChatMessage> messages;

        /**
         * @param id The ID of the {@link ChatMemory}.
         *           If not provided, a "default" will be used.
         * @return builder
         */
        public AppChatMemory.Builder id(Object id) {
            this.id = id;
            return this;
        }


        public AppChatMemory.Builder messages(List<ChatMessage> messages) {
            this.messages = messages;
            return this;
        }

        public AppChatMemory build() {
            return new AppChatMemory(this);
        }
    }

    public static AppChatMemory withMessages(List<ChatMessage> messages) {
        return builder().messages(messages).build();
    }
}
