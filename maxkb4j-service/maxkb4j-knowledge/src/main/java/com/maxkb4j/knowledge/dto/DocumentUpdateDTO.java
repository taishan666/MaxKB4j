package com.maxkb4j.knowledge.dto;

import lombok.Data;

/**
 * 文档更新入参：仅包含客户端可编辑字段；
 * status / charLength / knowledgeId 等由服务端维护。
 *
 * @author tarzan
 */
@Data
public class DocumentUpdateDTO {

    private String name;

    private Boolean isActive;

    private String hitHandlingMethod;

    private Double directlyReturnSimilarity;
}
