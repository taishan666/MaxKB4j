package com.tarzan.maxkb4j.module.functionlib.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class McpToolParams {

    private String server;
    private String name;
    private String description;
    private JSONObject args_schema;
}
