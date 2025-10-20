package com.tarzan.maxkb4j.core.workflow.node.http.impl;


import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.common.util.StringUtil;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.node.http.input.HttpNodeParams;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;

import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.HTTP_CLIENT;

public class HttpNode extends INode {


    public HttpNode(JSONObject properties) {
        super(properties);
        super.setType(HTTP_CLIENT.getKey());
    }


    @Override
    protected void saveContext(JSONObject detail) {
        context.put("status",detail.get("status"));
        context.put("body",detail.get("body"));
    }


}

