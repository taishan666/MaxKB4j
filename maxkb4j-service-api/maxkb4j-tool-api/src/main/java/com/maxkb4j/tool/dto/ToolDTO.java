package com.maxkb4j.tool.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class ToolDTO {
    private String id;
    private String name;
    private String icon;
    private String desc;
    private String toolType;
    private String code;
    private JSONObject initParams;
    private String userId;
    private Boolean isActive;
}
