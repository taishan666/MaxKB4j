package com.tarzan.maxkb4j.module.knowledge.domain.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.common.base.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "knowledge_action", autoResultMap = true)
public class KnowledgeActionEntity extends BaseEntity {
    private String state;
    private JSONObject details;
    private Float runTime;
    private JSONObject meta;
    private String knowledgeId;
}
