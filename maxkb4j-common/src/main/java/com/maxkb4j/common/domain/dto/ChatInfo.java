package com.maxkb4j.common.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ChatInfo implements Serializable {

    private String chatId;
    private String appId;
    private List<ChatRecordDTO> chatRecordList;
    private Map<String, Object> chatVariables = new HashMap<>(10);


    public ChatInfo(String chatId, String appId) {
        this.chatId = chatId;
        this.appId = appId;
        this.chatRecordList = new ArrayList<>();
    }

    public void addChatRecord(ChatRecordDTO chatRecord) {
        if (this.chatRecordList == null || this.chatRecordList.isEmpty()) {
            this.chatRecordList = new ArrayList<>();
        }
        this.chatRecordList.add(chatRecord);
    }

}
