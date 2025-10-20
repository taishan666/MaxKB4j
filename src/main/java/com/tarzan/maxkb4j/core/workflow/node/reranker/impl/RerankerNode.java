package com.tarzan.maxkb4j.core.workflow.node.reranker.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.RERANKER;

public class RerankerNode extends INode {


    public RerankerNode(JSONObject properties) {
        super(properties);
        this.setType(RERANKER.getKey());
    }


    @Override
    public void saveContext(Workflow workflow, JSONObject detail) {
        context.put("result", detail.get("result"));
        context.put("resultList", detail.get("resultList"));
    }


}
