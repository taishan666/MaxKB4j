package com.tarzan.maxkb4j.module.tool.enums;

import lombok.Getter;

@Getter
public enum ToolType {

    MCP("MCP"),
    CUSTOM("CUSTOM"),
    ;

    private final String key;

    ToolType(String key) {
        this.key = key;
    }

}
