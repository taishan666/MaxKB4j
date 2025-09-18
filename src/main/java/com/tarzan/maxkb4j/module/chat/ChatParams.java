package com.tarzan.maxkb4j.module.chat;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tarzan.maxkb4j.core.workflow.domain.ChatFile;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import lombok.Builder;
import lombok.Data;
import reactor.core.publisher.Sinks;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Builder
@Data
public class ChatParams {

    @NotBlank(message = "用户问题不能为空")
    private String message;

    @NotBlank(message = "对话id不能为空")
    private String chatId;

    @NotBlank(message = "对话记录id不能为空")
    private String chatRecordId;

    private String runtimeNodeId;

    @NotNull(message = "流式输出不能为空")
    private Boolean stream;

    private JSONObject formData;
    private JSONObject nodeData;

    private List<ChatFile> audioList;
    private List<ChatFile> documentList;
    private List<ChatFile> imageList;
    private List<ChatFile> otherList;

    @NotNull(message = "答案不能为空")
    private Boolean reChat;

    @JsonIgnore
    private Sinks.Many<ChatMessageVO> sink;
    @JsonIgnore
    private boolean debug;

}