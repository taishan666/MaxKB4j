package com.tarzan.maxkb4j.module.chat.dto;

import lombok.Data;

import java.util.Map;

@Data
public class McpResponse {
    public String jsonrpc = "2.0";
    public Object result;
    public Map<String,Object> error;
    public String id;
}