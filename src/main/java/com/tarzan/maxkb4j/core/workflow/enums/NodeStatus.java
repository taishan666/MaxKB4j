package com.tarzan.maxkb4j.core.workflow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum NodeStatus {

    READY(100,"等待执行"),
    SUCCESS(200,"执行成功"),
    INTERRUPT(202,"暂停中断"),
    SKIP(300,"跳过"),
    ERROR(500,"发生错误"),
    ;

    private final Integer code;

    private final String name;
}
