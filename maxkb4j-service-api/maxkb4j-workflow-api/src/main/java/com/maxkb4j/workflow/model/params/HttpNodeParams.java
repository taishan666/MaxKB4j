package com.maxkb4j.workflow.model.params;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.List;

/**
 * HTTP 请求节点参数
 * 从 HttpNode.NodeParams 提取
 */
@Data
public class HttpNodeParams {
    private String url;
    private String method;
    private JSONObject headers;
    private JSONObject body;
    private List<String> queryParams;
    private Boolean sslVerify;
    private Integer timeout;
}