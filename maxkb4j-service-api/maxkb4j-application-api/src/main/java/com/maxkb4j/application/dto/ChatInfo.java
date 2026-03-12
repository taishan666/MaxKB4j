package com.maxkb4j.application.dto;

import com.maxkb4j.application.entity.ApplicationChatRecordEntity;
import com.maxkb4j.common.domain.dto.ChatRecordDTO;
import com.maxkb4j.common.util.BeanUtil;
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
    private List<ChatRecordDTO> chatRecordList = new ArrayList<>();

    public void addChatRecord(ApplicationChatRecordEntity chatRecord) {
        this.chatRecordList.add(BeanUtil.copy(chatRecord, ChatRecordDTO.class));
    }

    public void setChatRecordList(List<ApplicationChatRecordEntity> chatRecordList) {
        this.chatRecordList = BeanUtil.copyList(chatRecordList, ChatRecordDTO.class);
    }
}
