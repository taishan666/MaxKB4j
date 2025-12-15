package com.tarzan.maxkb4j.core.workflow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

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
