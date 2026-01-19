package com.tarzan.maxkb4j.module.tool.enums;

import lombok.Getter;

@Getter
public enum ToolType {

    MCP("MCP"),
    CUSTOM("CUSTOM"),
    ;

    private final String value;

    ToolType(String value) {
        this.value = value;
    }

}
