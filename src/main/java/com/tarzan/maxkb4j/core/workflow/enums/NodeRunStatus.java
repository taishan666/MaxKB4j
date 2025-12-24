package com.tarzan.maxkb4j.core.workflow.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum NodeRunStatus {

    READY,
    SUCCESS,
    INTERRUPT,
    SKIP,
    ERROR,
    ;
}
