package com.tarzan.maxkb4j.module.knowledge.domain.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.common.base.entity.BaseEntity;
import com.tarzan.maxkb4j.common.typehandler.JSONBTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author tarzan
 * @date 2025-12-20 16:00:15
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "knowledge_workflow_version", autoResultMap = true)
public class KnowledgeVersionEntity extends BaseEntity {
    private String name;
    private String publishUserId;
    private String publishUserName;
    private String knowledgeId;
    @TableField(typeHandler = JSONBTypeHandler.class)
    private JSONObject workFlow;
}
