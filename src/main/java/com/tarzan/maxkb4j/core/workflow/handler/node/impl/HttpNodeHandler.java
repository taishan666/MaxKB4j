package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.common.util.StringUtil;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.node.http.impl.HttpNode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HttpNodeHandler implements INodeHandler {

    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        HttpNode.NodeParams nodeParams=node.getNodeData().toJavaObject(HttpNode.NodeParams.class);
        HttpRequest request= HttpUtil.createRequest(nodeParams.getMethod(), nodeParams.getUrl());
        node.getDetail().put("url",nodeParams.getUrl());
        node.getDetail().put("method",nodeParams.getMethod());
        JSONArray headers=nodeParams.getHeaders();
        node.getDetail().put("headers",headers);
        for (int i = 0; i < headers.size(); i++) {
            JSONObject header=headers.getJSONObject(i);
            request.header(header.getString("name"),header.getString("value"));
        }
        String body=nodeParams.getBody();
        node.getDetail().put("requestBody",body);
        if (StringUtil.isNotBlank(body)){
            request.body(body);
        }
        JSONArray params=nodeParams.getParams();
        node.getDetail().put("params",params);
        for (int i = 0; i < params.size(); i++) {
            JSONObject param=params.getJSONObject(i);
            request.form(param.getString("name"),param.getString("value"));
        }
        if (StringUtil.isNotBlank(nodeParams.getAuthType())){
            switch (nodeParams.getAuthType()){
                case "basic":
                    request.basicAuth(nodeParams.getUsername(),nodeParams.getPassword());
                    break;
                case "bearer":
                    request.bearerAuth(nodeParams.getToken());
                    break;
            }
        }
        request.timeout(nodeParams.getTimeout()*1000);
        node.getDetail().put("timeout",nodeParams.getTimeout());
        HttpResponse response=request.execute();
        return new NodeResult(Map.of("status",response.getStatus(),"body",response.body()),Map.of());
    }
}
