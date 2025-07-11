package com.tarzan.maxkb4j.module.application.domian.dto;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.domain.ChatFile;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import lombok.Builder;
import lombok.Data;
import reactor.core.publisher.Sinks;

import java.util.List;

@Builder
@Data
public class ChatMessageDTO {
    private String message;
    private String chatRecordId;
    private String clientId;
    private String clientType;
    private Boolean stream;
    private Boolean reChat;
    private JSONObject formData;
    private JSONObject nodeData;
    private String runtimeNodeId;
    private List<ChatFile> audioList;
    private List<ChatFile> documentList;
    private List<ChatFile> imageList;
    // private List<JSONObject> videoList;
    private Sinks.Many<ChatMessageVO> sink;
}
