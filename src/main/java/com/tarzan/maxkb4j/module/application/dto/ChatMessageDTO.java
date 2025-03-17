package com.tarzan.maxkb4j.module.application.dto;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ChatMessageDTO {
    private String message;
    private String chatRecordId;
    private Boolean stream;
    private Boolean reChat;
    @JsonProperty("form_data")
    private JSONObject formData;
    @JsonProperty("node_data")
    private JSONObject nodeData;
    private String runtimeNodeId;
    private List<JSONObject> audioList;
    private List<JSONObject> documentList;
    private List<JSONObject> imageList;
    private List<JSONObject> videoList;

}
