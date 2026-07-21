package com.maxkb4j.chat.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class McpRequest {
    public String method;
    public JSONObject params;
    public Object id;
}
