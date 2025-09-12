package com.tarzan.maxkb4j.core.workflow.node.http.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.http.input.HttpNodeParams;
import com.tarzan.maxkb4j.util.StringUtil;

import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.HTTP_CLIENT;

public class BaseHttpNode extends INode {


    public BaseHttpNode(JSONObject properties) {
        super(properties);
    }

    @Override
    public NodeResult execute() throws Exception {
        System.out.println(HTTP_CLIENT);
        HttpNodeParams nodeParams=super.nodeParams.toJavaObject(HttpNodeParams.class);
        HttpRequest request=HttpUtil.createRequest(nodeParams.getMethod(), nodeParams.getUrl());
        JSONArray headers=nodeParams.getHeaders();
        for (int i = 0; i < headers.size(); i++) {
             JSONObject header=headers.getJSONObject(i);
             request.header(header.getString("name"),header.getString("value"));
        }
        if (StringUtil.isNotBlank(nodeParams.getBody())){
            request.body(nodeParams.getBody());
        }
        JSONArray params=nodeParams.getParams();
        for (int i = 0; i < params.size(); i++) {
            JSONObject param=headers.getJSONObject(i);
            request.header(param.getString("name"),param.getString("value"));
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
        HttpResponse response=request.execute();
        System.out.println( response.body());
        return new NodeResult(Map.of("status",response.getStatus(),"body",response.body()),Map.of());
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("status",context.get("status"));
        detail.put("body",context.get("body"));
        return detail;
    }
}
