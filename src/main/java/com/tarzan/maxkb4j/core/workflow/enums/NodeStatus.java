package com.tarzan.maxkb4j.core.workflow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum NodeStatus {

    READY(0,"就绪"),

    RUNNING(1,"执行中"),

    ERROR(500,"发生错误"),

    SUCCESS(200,"执行成功"),

    FAIL_END(-1,"错误结束"),
    ;
    private final Integer code;

    private final String name;
}
