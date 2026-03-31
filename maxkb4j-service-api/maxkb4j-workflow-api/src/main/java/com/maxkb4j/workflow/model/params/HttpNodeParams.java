package com.maxkb4j.workflow.model.params;

import com.alibaba.fastjson.JSONArray;
import lombok.Data;

/**
 * HTTP 请求节点参数
 * 从 HttpNode.NodeParams 提取，保持字段定义一致
 */
@Data
public class HttpNodeParams {
    private String url;
    private String method;
    private String body;
    private JSONArray headers;
    private JSONArray params;
    private Integer timeout;
    private String authType;
    private String username;
    private String password;
    private String token;
}