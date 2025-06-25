package com.tarzan.maxkb4j.core.workflow.node.http.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.http.input.HttpNodeParams;
import com.tarzan.maxkb4j.util.StringUtil;

import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.HTTP_CLIENT;

public class BaseHttpNode extends INode {


    @Override
    public NodeResult execute() throws Exception {
        System.out.println(HTTP_CLIENT);
        HttpNodeParams nodeParams=super.nodeParams.toJavaObject(HttpNodeParams.class);
        HttpRequest request=HttpUtil.createRequest(nodeParams.getMethod(), nodeParams.getUrl());
        Map<String, String> headers=nodeParams.getHeaders();
        headers.forEach(request::header);
        if (StringUtil.isNotBlank(nodeParams.getBody())){
            request.body(nodeParams.getBody());
        }
        if (CollectionUtil.isNotEmpty(nodeParams.getParams())){
            request.form(nodeParams.getParams());
        }
        request.timeout(nodeParams.getTimeout());
        HttpResponse response=request.execute();
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
