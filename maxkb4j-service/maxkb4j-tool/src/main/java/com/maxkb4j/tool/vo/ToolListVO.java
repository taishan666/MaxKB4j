package com.maxkb4j.tool.vo;

import lombok.Data;

@Data
public class ToolListVO {
    private String id;
    private String name;
    private String icon;
    private String toolType;
    private Boolean isActive;
}
