package com.tarzan.maxkb4j.core.workflow.node.impl;


import cn.hutool.http.Method;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import lombok.Data;

import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.HTTP_CLIENT;

public class HttpNode extends INode {


    public HttpNode(String id,JSONObject properties) {
        super(id,properties);
        super.setType(HTTP_CLIENT.getKey());
    }


    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        context.put("status",detail.get("status"));
        context.put("body",detail.get("body"));
    }

    @Data
    public static class NodeParams {
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


}

