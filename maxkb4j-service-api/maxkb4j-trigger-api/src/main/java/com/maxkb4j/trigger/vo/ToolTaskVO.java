package com.maxkb4j.trigger.vo;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class ToolTaskVO {
    private String id;
    private String name;
    private String icon;
    private String toolType;
    private JSONObject workFlow;
}
