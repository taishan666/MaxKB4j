package com.maxkb4j.common.domain.vo;

import lombok.Data;

import java.util.Map;

@Data
public class McpResponse {
    public String jsonrpc = "2.0";
    public Object result;
    public Map<String,Object> error;
    public Object id;
}