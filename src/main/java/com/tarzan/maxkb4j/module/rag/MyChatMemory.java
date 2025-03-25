package com.tarzan.maxkb4j.module.rag;

import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import org.springframework.util.CollectionUtils;

import java.util.*;

public class MyChatMemory implements ChatMemory {

    private final Integer maxMessages;
    private final Integer maxTokens;
    private final Map<Object,List<ChatMessage>> chatStore;

    public MyChatMemory(Integer maxMessages,Integer maxTokens) {
        this.maxMessages = maxMessages;
        this.maxTokens = maxTokens;
        this.chatStore = new HashMap<>();
    }

    @Override
    public Object id() {
        return "default";
    }

    public void add1(List<ChatMessage> messages) {
        Optional<SystemMessage> systemMessage = findSystemMessage(messages);
        systemMessage.ifPresent(messages::remove);
        chatStore.put(id(),messages);
    }

    public void add(List<ApplicationChatRecordEntity> chatRecordList) {
        List<ChatMessage> messages = new LinkedList<>();
        if (!CollectionUtils.isEmpty(chatRecordList)){
            int startIndex = chatRecordList.size() - maxMessages;
            if (startIndex > 0) {
                chatRecordList = chatRecordList.subList(startIndex, chatRecordList.size());
            }
            for (ApplicationChatRecordEntity chatRecord : chatRecordList) {
                messages.add(UserMessage.from(chatRecord.getProblemText()));
                messages.add(AiMessage.from(chatRecord.getAnswerText()));
            }
            Optional<SystemMessage> systemMessage = findSystemMessage(messages);
            systemMessage.ifPresent(messages::remove);
           // ensureCapacity(messages,maxMessages);
            chatStore.put(id(),messages);
        }
    }


    @Override
    public void add(ChatMessage message) {
        List<ChatMessage> messages = this.messages();
        if (message instanceof SystemMessage) {
            Optional<SystemMessage> systemMessage = findSystemMessage(messages);
            if (systemMessage.isPresent()) {
                if (systemMessage.get().equals(message)) {
                    return;
                }
                messages.remove(systemMessage.get());
            }
        }
        messages.add(message);
        chatStore.put(id(),messages);
    }

    private static Optional<SystemMessage> findSystemMessage(List<ChatMessage> messages) {
        return messages.stream().filter((message) -> message instanceof SystemMessage).map((message) -> (SystemMessage)message).findAny();
    }


    private static void ensureCapacity(List<ChatMessage> messages, int maxMessages) {
        while(messages.size() > maxMessages) {
            int messageToEvictIndex = 0;
            if (messages.get(0) instanceof SystemMessage) {
                messageToEvictIndex = 1;
            }
            messages.remove(messageToEvictIndex);
        }
    }

    @Override
    public List<ChatMessage> messages() {
        return new LinkedList<>(chatStore.getOrDefault(id(),new ArrayList<>()));
    }

    @Override
    public void clear() {
        chatStore.remove(id());
    }


}
