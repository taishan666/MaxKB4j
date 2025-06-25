package com.tarzan.maxkb4j.core.workflow.node.http.input;

import cn.hutool.http.Method;
import lombok.Data;

import java.util.Map;

@Data
public class HttpNodeParams {
    private String url;
    private Method method;
    private String body;
    private Map<String, String> headers;
    private Map<String, Object> params;
    private Integer timeout;
}
