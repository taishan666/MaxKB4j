package com.tarzan.maxkb4j.module.application.domain.dto;

import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationChatRecordEntity;
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
    private List<ApplicationChatRecordEntity> chatRecordList = new ArrayList<>();

    public void addChatRecord(ApplicationChatRecordEntity chatRecord) {
        this.chatRecordList.add(chatRecord);
    }
}
