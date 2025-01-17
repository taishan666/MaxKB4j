package com.tarzan.maxkb4j.module.dataset.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.handler.UUIDTypeHandler;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;
@EqualsAndHashCode(callSuper = true)
@Data
public class ProblemDTO extends ProblemEntity {
    @TableField(typeHandler = UUIDTypeHandler.class)
    private UUID paragraphId;
    @JsonProperty("document_id")
    @TableField(typeHandler = UUIDTypeHandler.class)
    private UUID documentId;
}
