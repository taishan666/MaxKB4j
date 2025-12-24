package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.node.impl.HttpNode;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

@NodeHandlerType(NodeType.HTTP_CLIENT)
@Component
public class HttpNodeHandler implements INodeHandler {

    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        HttpNode.NodeParams nodeParams=node.getNodeData().toJavaObject(HttpNode.NodeParams.class);
        HttpRequest request= HttpUtil.createRequest(nodeParams.getMethod(), nodeParams.getUrl());
        node.getDetail().put("url",nodeParams.getUrl());
        node.getDetail().put("method",nodeParams.getMethod());
        JSONArray headers=nodeParams.getHeaders();
        for (int i = 0; i < headers.size(); i++) {
            JSONObject header=headers.getJSONObject(i);
            if (!header.isEmpty()){
                request.header(header.getString("name"),header.getString("value"));
            }
        }
        node.getDetail().put("headers",request.headers());
        String body=nodeParams.getBody();
        node.getDetail().put("requestBody",body);
        if (StringUtils.isNotBlank(body)){
            request.body(body);
        }
        JSONArray params=nodeParams.getParams();
        for (int i = 0; i < params.size(); i++) {
            JSONObject param=params.getJSONObject(i);
            if (!param.isEmpty()){
                request.form(param.getString("name"),param.getString("value"));
            }
        }
        node.getDetail().put("params",request.form());
        if (StringUtils.isNotBlank(nodeParams.getAuthType())){
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
        return new NodeResult(Map.of("status",response.getStatus(),"body",response.body()));
    }
}
