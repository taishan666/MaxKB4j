package com.tarzan.maxkb4j.module.rag;

import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.service.memory.ChatMemoryService;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static dev.langchain4j.internal.ValidationUtils.ensureGreaterThanZero;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

public class MyChatMemory implements ChatMemory {

    private final Object id;
    private final Integer maxMessages;
    private final ChatMemoryStore store;

    private MyChatMemory(MyChatMemory.Builder builder) {
        this.id = ensureNotNull(builder.id, "id");
        this.maxMessages = ensureGreaterThanZero(builder.maxMessages+1, "maxMessages");
        this.store = ensureNotNull(builder.store(), "store");
    }

    @Override
    public Object id() {
        return id;
    }

    @Override
    public void add(ChatMessage message) {
        List<ChatMessage> messages = messages();
        if (message instanceof SystemMessage) {
            Optional<SystemMessage> systemMessage = findSystemMessage(messages);
            if (systemMessage.isPresent()) {
                if (systemMessage.get().equals(message)) {
                    return; // do not add the same system message
                } else {
                    messages.remove(systemMessage.get()); // need to replace existing system message
                }
            }
        }
        messages.add(message);
        ensureCapacity(messages, maxMessages);
        store.updateMessages(id, messages);
    }

    private static Optional<SystemMessage> findSystemMessage(List<ChatMessage> messages) {
        return messages.stream()
                .filter(message -> message instanceof SystemMessage)
                .map(message -> (SystemMessage) message)
                .findAny();
    }

    @Override
    public List<ChatMessage> messages() {
        List<ChatMessage> messages = new LinkedList<>(store.getMessages(id));
        ensureCapacity(messages, maxMessages);
        return messages;
    }

    private static void ensureCapacity(List<ChatMessage> messages, int maxMessages) {
        long userMessagesCount = messages.stream()
                .filter(message -> message instanceof UserMessage)
                .count();
        while (userMessagesCount > maxMessages) {
            int messageToEvictIndex = 0;
            if (messages.get(0) instanceof SystemMessage) {
                messageToEvictIndex = 1;
            }
            ChatMessage evictedMessage = messages.remove(messageToEvictIndex);
            if (evictedMessage instanceof AiMessage aiMessage) {
                if (aiMessage.hasToolExecutionRequests()){
                    while (messages.size() > messageToEvictIndex
                            && messages.get(messageToEvictIndex) instanceof ToolExecutionResultMessage) {
                        // Some LLMs (e.g. OpenAI) prohibit ToolExecutionResultMessage(s) without corresponding AiMessage,
                        // so we have to automatically evict orphan ToolExecutionResultMessage(s) if AiMessage was evicted
                        messages.remove(messageToEvictIndex);
                    }
                }else {
                    userMessagesCount = messages.stream()
                            .filter(message -> message instanceof UserMessage)
                            .count();
                }
            }

        }
    }


    @Override
    public void clear() {
        store.deleteMessages(id);
    }

    public static MyChatMemory.Builder builder() {
        return new MyChatMemory.Builder();
    }

    public static class Builder {

        private Object id = ChatMemoryService.DEFAULT;
        private Integer maxMessages;
        private ChatMemoryStore store;

        /**
         * @param id The ID of the {@link ChatMemory}.
         *           If not provided, a "default" will be used.
         * @return builder
         */
        public MyChatMemory.Builder id(Object id) {
            this.id = id;
            return this;
        }

        /**
         * @param maxMessages The maximum number of messages to retain.
         *                    If there isn't enough space for a new message, the oldest one is evicted.
         * @return builder
         */
        public MyChatMemory.Builder maxMessages(Integer maxMessages) {
            this.maxMessages = maxMessages;
            return this;
        }

        /**
         * @param store The chat memory store responsible for storing the chat memory state.
         *              If not provided, an {@link MyChatMemoryStore} will be used.
         * @return builder
         */
        public MyChatMemory.Builder chatMemoryStore(ChatMemoryStore store) {
            this.store = store;
            return this;
        }

        private ChatMemoryStore store() {
            return store != null ? store : new MyChatMemoryStore();
        }

        public MyChatMemory build() {
            return new MyChatMemory(this);
        }
    }

    public static MyChatMemory withMaxMessages(int maxMessages) {
        return builder().maxMessages(maxMessages).build();
    }
}
