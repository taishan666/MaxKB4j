package com.tarzan.maxkb4j.module.dataset.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.handler.UUIDTypeHandler;
import com.tarzan.maxkb4j.module.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.UUID;

/**
 * @author tarzan
 * @date 2024-12-26 10:45:40
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("problem")
public class ProblemEntity extends BaseEntity {
    
    private String content;
    
	@JsonProperty("hit_num")
    private Integer hitNum;
    
	@JsonProperty("dataset_id")
    @TableField(typeHandler = UUIDTypeHandler.class)
    private UUID datasetId;

}
