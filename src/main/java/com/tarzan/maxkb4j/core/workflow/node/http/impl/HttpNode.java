package com.tarzan.maxkb4j.core.workflow.node.http.impl;


import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.HTTP_CLIENT;

public class HttpNode extends INode {


    public HttpNode(JSONObject properties) {
        super(properties);
        super.setType(HTTP_CLIENT.getKey());
    }


    @Override
    protected void saveContext(Workflow workflow, JSONObject detail) {
        context.put("status",detail.get("status"));
        context.put("body",detail.get("body"));
    }


}

