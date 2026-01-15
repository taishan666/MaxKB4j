package com.tarzan.maxkb4j.module.chat.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class McpRequest {
    public String method;
    public JSONObject params;
    public String id;
}
