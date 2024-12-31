package com.tarzan.maxkb4j.module.dataset.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.handler.UUIDTypeHandler;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemParagraphEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProblemParagraphVO extends ProblemParagraphEntity {
    private String content;
    @TableField(typeHandler = UUIDTypeHandler.class)
    private UUID datasetId;
    @JsonProperty("document_id")
    @TableField(typeHandler = UUIDTypeHandler.class)
    private UUID documentId;
    @TableField(typeHandler = UUIDTypeHandler.class)
    private UUID paragraphId;
    @TableField(typeHandler = UUIDTypeHandler.class)
    private UUID problemId;
}
