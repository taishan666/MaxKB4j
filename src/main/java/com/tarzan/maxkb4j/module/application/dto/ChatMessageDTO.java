package com.tarzan.maxkb4j.module.application.dto;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ChatMessageDTO {
    private String message;
    @JsonProperty("chat_record_id")
    private String chatRecordId;
    private Boolean stream;
    @JsonProperty("re_chat")
    private Boolean reChat;
    @JsonProperty("form_data")
    private JSONObject formData;
    @JsonProperty("node_data")
    private JSONObject nodeData;
    @JsonProperty("runtime_node_id")
    private String runtimeNodeId;
    @JsonProperty("audio_list")
    private List<JSONObject> audioList;
    @JsonProperty("document_list")
    private List<JSONObject> documentList;
    @JsonProperty("image_list")
    private List<JSONObject> imageList;
    @JsonProperty("video_list")
    private List<JSONObject> videoList;

}
