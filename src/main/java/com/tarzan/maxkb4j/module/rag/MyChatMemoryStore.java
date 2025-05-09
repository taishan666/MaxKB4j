package com.tarzan.maxkb4j.module.rag;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.util.List;

public class MyChatMemoryStore implements ChatMemoryStore {


    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        return null;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
    }

    @Override
    public void deleteMessages(Object memoryId) {
    }

}
