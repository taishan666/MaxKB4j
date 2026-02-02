package com.tarzan.maxkb4j.core.workflow.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum NodeStatus {

    READY(100),
    SUCCESS(200),
    STARTED(202),
    SKIP(205),
    INTERRUPT(206),
    ERROR(500),
    ;

    private final int status;
}
