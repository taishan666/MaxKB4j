package com.tarzan.maxkb4j.module.knowledge.enums;

import lombok.Getter;

@Getter
public enum DocType {

    BASE(0, "通用类型"),
    WEB(1, "web站点类型"),
    LARK(2, "飞书类型"),
    YU_QUE(3,"飞书类型");

    private final Integer type;
    private final String desc;

    DocType(Integer type,String desc) {
        this.type = type;
        this.desc = desc;
    }
}
