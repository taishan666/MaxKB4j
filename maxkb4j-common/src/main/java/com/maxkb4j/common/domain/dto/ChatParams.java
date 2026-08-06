package com.maxkb4j.common.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 对话请求入参：仅承载客户端提交的请求字段，由 {@code @RequestBody} 绑定。
 * <p>
 * 服务端解析的运行时状态（用户身份、来源、历史记录等）已分离至 {@link ChatState}，
 * 沿调用链独立传递，避免请求 DTO 与可变执行上下文混装。
 *
 * @author tarzan
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    private List<OssFile> audioList;
    @Schema(description = "文档列表")
    private List<OssFile> documentList;
    @Schema(description = "图片列表")
    private List<OssFile> imageList;
    @Schema(description = "其他列表")
    private List<OssFile> otherList;
    @Schema(description = "是否重新回答")
    @NotNull(message = "是否重新回答")
    private Boolean reChat;
}
