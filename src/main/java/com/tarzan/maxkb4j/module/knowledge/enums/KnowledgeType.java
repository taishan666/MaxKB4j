package com.tarzan.maxkb4j.module.knowledge.enums;

import lombok.Getter;

@Getter
public enum KnowledgeType {

    BASE(0, "通用类型"),
    WEB(1, "web站点类型"),
    WORKFLOW(2, "工作流类型"),
    ;

    private final Integer type;
    private final String desc;

    KnowledgeType(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }
}
