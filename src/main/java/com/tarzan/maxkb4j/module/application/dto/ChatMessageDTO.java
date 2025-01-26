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
    @JsonProperty("re_chat")
    private Boolean reChat;
    @JsonProperty("form_data")
    private JSONObject formData;
    @JsonProperty("audio_list")
    private List<Object> audioList;
    @JsonProperty("document_list")
    private List<Object> documentList;
    @JsonProperty("image_list")
    private List<Object> imageList;
    @JsonProperty("video_list")
    private List<Object> videoList;

}
