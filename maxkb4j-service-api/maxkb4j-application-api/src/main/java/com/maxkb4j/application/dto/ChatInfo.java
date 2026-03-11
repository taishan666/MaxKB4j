package com.maxkb4j.application.dto;

import com.maxkb4j.common.domain.entity.ChatRecordEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ChatInfo implements Serializable {

    private String chatId;
    private String appId;
    private Map<String, Object> chatVariables = new HashMap<>(10);
    private List<ChatRecordEntity> chatRecordList = new ArrayList<>();

    public void addChatRecord(ChatRecordEntity chatRecord) {
        this.chatRecordList.add(chatRecord);
    }
}
