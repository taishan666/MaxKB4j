package com.tarzan.maxkb4j.module.rag;

import com.tarzan.maxkb4j.module.application.service.ApplicationChatRecordService;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@AllArgsConstructor
@Component
public class PersistentChatMemoryStore implements ChatMemoryStore {

    private final ApplicationChatRecordService chatRecordService;
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        return List.of();
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> list) {

    }

    @Override
    public void deleteMessages(Object memoryId) {

    }
}
