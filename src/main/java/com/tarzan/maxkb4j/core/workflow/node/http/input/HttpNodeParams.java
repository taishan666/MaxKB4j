package com.tarzan.maxkb4j.core.workflow.node.http.input;

import cn.hutool.http.Method;
import com.alibaba.fastjson.JSONArray;
import lombok.Data;

@Data
public class HttpNodeParams {
    private String url;
    private Method method;
    private String body;
    private JSONArray headers;
    private JSONArray params;
    private Integer timeout;
    private String authType;
    private String username;
    private String password;
    private String token;
}
