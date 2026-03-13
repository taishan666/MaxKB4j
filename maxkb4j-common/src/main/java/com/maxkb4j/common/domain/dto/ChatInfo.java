package com.maxkb4j.common.domain.dto;

import lombok.Data;
import lombok.Setter;

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
    @Setter
    private List<ChatRecordDTO> chatRecordList = new ArrayList<>();

    public void addChatRecord(ChatRecordDTO chatRecord) {
        this.chatRecordList.add(chatRecord);
    }

}
