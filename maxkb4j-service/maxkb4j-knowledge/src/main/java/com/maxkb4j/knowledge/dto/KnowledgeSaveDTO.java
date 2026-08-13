package com.maxkb4j.knowledge.dto;

import com.alibaba.fastjson.JSONObject;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 知识库创建/更新入参：仅包含客户端可提供的字段；
 * userId 由服务端强制设置，id 来自路径参数。
 *
 * @author tarzan
 */
@Data
public class KnowledgeSaveDTO {

    @NotBlank(message = "知识库名称不能为空")
    private String name;

    private String desc;

    private String embeddingModelId;

    private Integer fileSizeLimit;

    private Integer fileCountLimit;

    private String folderId;

    private JSONObject meta;

    private JSONObject workFlow;

    private Boolean isPublish;
}
