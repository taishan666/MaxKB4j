package com.tarzan.maxkb4j.module.rag;

import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import org.springframework.util.CollectionUtils;

import java.util.LinkedList;
import java.util.List;

public class MyChatMemory implements ChatMemory {

    private final List<ChatMessage> messages = new LinkedList<>();

    public MyChatMemory(List<ApplicationChatRecordEntity> chatRecordList,Integer maxMessages) {
        if (!CollectionUtils.isEmpty(chatRecordList)) {
            int startIndex = chatRecordList.size() - maxMessages;
            if (startIndex > 0) {
                chatRecordList = chatRecordList.subList(startIndex, chatRecordList.size());
            }
            for (ApplicationChatRecordEntity chatRecord : chatRecordList) {
                this.messages.add(UserMessage.from(chatRecord.getProblemText()));
                this.messages.add(AiMessage.from(chatRecord.getAnswerText()));
            }
        }
    }

    @Override
    public Object id() {
        return "default";
    }


    @Override
    public void add(ChatMessage message) {
        this.messages.add(message);
    }



    @Override
    public List<ChatMessage> messages() {
        return messages;
    }

    @Override
    public void clear() {

    }


}
