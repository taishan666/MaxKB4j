package com.tarzan.maxkb4j.module.chat.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tarzan.maxkb4j.core.workflow.model.ChatFile;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Builder
@Data
@Schema(description = "对话参数", requiredProperties = {"message", "chatId"})
public class ChatParams {

    @Schema(description = "用户问题")
    @NotBlank(message = "用户问题不能为空")
    private String message;
    @Schema(description = "对话id")
    @NotBlank(message = "对话id不能为空")
    private String chatId;
    @Schema(description = "对话记录id")
    private String chatRecordId;
    @Schema(description = "运行节点id")
    private String runtimeNodeId;
    @Schema(description = "是否流式响应,默认为false")
    private Boolean stream;
    @Schema(description = "表单数据", example = "{ \"name\": \"张三\", \"age\": 25 }")
    private Map<String,Object> formData;
    @Schema(description = "节点数据", example = "{ \"name\": \"张三\", \"age\": 25 }")
    private Map<String,Object> nodeData;
    @Schema(description = "子节点对象", implementation = ChildNode.class)
    private ChildNode childNode;
    @Schema(description = "音频列表")
    private List<ChatFile> audioList;
    @Schema(description = "文档列表")
    private List<ChatFile> documentList;
    @Schema(description = "图片列表")
    private List<ChatFile> imageList;
    @Schema(description = "其他列表")
    private List<ChatFile> otherList;
    @Schema(description = "是否重新回答")
    @NotNull(message = "是否重新回答")
    private Boolean reChat;

    @JsonIgnore
    private String appId;
    @JsonIgnore
    private Boolean debug;
    @JsonIgnore
    private String chatUserId;
    @JsonIgnore
    private String chatUserType;
    @JsonIgnore
    private List<ApplicationChatRecordEntity> historyChatRecords;
    @JsonIgnore
    private ApplicationChatRecordEntity chatRecord;


}