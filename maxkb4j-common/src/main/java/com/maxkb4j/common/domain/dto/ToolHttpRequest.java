package com.maxkb4j.common.domain.dto;

import cn.hutool.http.Method;
import lombok.Data;

import java.util.List;

@Data
public class ToolHttpRequest {
    private String url;
    private Method method;
    private String body;
    private List<KeyAndValue> headers;
    private List<KeyAndValue> params;
    private Integer timeout;
    private String authType;
    private String username;
    private String password;
    private String token;
}
