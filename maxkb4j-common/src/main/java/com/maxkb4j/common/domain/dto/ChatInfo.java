package com.maxkb4j.common.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ChatInfo implements Serializable {

    private String chatId;
    private String appId;
    private List<ChatRecordDTO> chatRecordList = new CopyOnWriteArrayList<>();
    private Map<String, Object> chatVariables = new ConcurrentHashMap<>(10);


    public ChatInfo(String chatId, String appId) {
        this.chatId = chatId;
        this.appId = appId;
    }

    public void putChatVariables(Map<String, Object> chatVariables) {
        this.chatVariables.putAll(chatVariables);
    }


    public void addChatRecord(ChatRecordDTO chatRecord) {
        String chatRecordId = chatRecord.getId();
        // 存在相同 id 的记录时替换，否则添加
        for (int i = 0; i < this.chatRecordList.size(); i++) {
            if (this.chatRecordList.get(i).getId().equals(chatRecordId)) {
                this.chatRecordList.set(i, chatRecord);
                return;
            }
        }
        this.chatRecordList.add(chatRecord);
    }

}
