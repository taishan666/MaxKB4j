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
    private List<ChatRecordDTO> chatRecordList = new ArrayList<>();
    private Map<String, Object> chatVariables = new HashMap<>(10);


    public ChatInfo(String chatId, String appId) {
        this.chatId = chatId;
        this.appId = appId;
    }

    public void addChatRecord(ChatRecordDTO chatRecord) {
        this.chatRecordList.add(chatRecord);
    }

}
