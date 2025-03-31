package com.tarzan.maxkb4j.module.application.vo;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class McpToolVO {
    private String server;
    private String name;
    private String description;
    private JSONObject args_schema;
}
