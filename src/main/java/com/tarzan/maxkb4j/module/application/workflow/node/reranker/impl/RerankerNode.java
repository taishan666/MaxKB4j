package com.tarzan.maxkb4j.module.application.workflow.node.reranker.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.WorkflowManage;
import com.tarzan.maxkb4j.module.application.workflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.workflow.node.reranker.IRerankerNode;
import com.tarzan.maxkb4j.module.application.workflow.node.reranker.dto.RerankerParams;

import java.util.Map;

public class RerankerNode extends IRerankerNode {
    @Override
    public NodeResult execute(RerankerParams nodeParams, FlowParams workflowParams) {
        return new NodeResult(Map.of("answer", ""), Map.of());
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("document_list", context.get("document_list"));
        detail.put("question", context.get("question"));
        detail.put("reranker_setting", context.get("content"));
        detail.put("result", context.get("answer"));
        detail.put("result_list", context.get("audio_list"));
        return detail;
    }

    @Override
    public void saveContext(JSONObject nodeDetail, WorkflowManage workflowManage) {

    }
}
