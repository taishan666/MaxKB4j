package com.maxkb4j.tool.vo;

import lombok.Data;

import java.util.Date;

@Data
public class ToolCardVO {
    private String id;
    private String name;
    private String desc;
    private String icon;
    private String toolType;
    private Boolean isActive;
    private String templateId;
    private String version;
    private Date createTime;
    private String nickname;
    private int resourceCount;
}
