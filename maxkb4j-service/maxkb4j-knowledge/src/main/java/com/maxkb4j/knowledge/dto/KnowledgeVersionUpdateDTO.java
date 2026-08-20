package com.maxkb4j.knowledge.dto;

import lombok.Data;

/**
 * 知识库版本更新入参：仅允许改名；
 * 发布人 / 归属知识库 / 工作流快照不允许从客户端变更。
 *
 * @author tarzan
 */
@Data
public class KnowledgeVersionUpdateDTO {

    private String name;
}
