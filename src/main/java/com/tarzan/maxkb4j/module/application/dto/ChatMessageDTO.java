package com.tarzan.maxkb4j.module.application.dto;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.dto.ChatFile;
import lombok.Data;

import java.util.List;

@Data
public class ChatMessageDTO {
    private String message;
    private String chatRecordId;
    private Boolean stream;
    private Boolean reChat;
    private JSONObject formData;
    private JSONObject nodeData;
    private String runtimeNodeId;
    private List<ChatFile> audioList;
    private List<ChatFile> documentList;
    private List<ChatFile> imageList;
   // private List<JSONObject> videoList;

}
