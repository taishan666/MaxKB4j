package com.tarzan.maxkb4j.module.application.workflow.node.applicationnode.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.WorkflowManage;
import com.tarzan.maxkb4j.module.application.workflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.workflow.node.applicationnode.IApplicationNode;
import com.tarzan.maxkb4j.module.application.workflow.node.applicationnode.dto.ApplicationNodeParams;

public class BaseApplicationNode extends IApplicationNode {

    @Override
    public NodeResult execute(ApplicationNodeParams nodeParams, FlowParams workflowParams) {
        return null;
    }

    @Override
    public JSONObject getDetail(int index) {
        JSONObject detail = super.getDetail(index);
        detail.put("info", node.getProperties().getString("node_data"));
        detail.put("question", context.getString("question"));
        detail.put("answer", context.getString("answer"));
        detail.put("message_tokens", context.getString("message_tokens"));
        detail.put("answer_tokens", context.getString("answer_tokens"));
        detail.put("image_list", context.get("image"));
        detail.put("document_list", context.get("document"));
        detail.put("audio_list", context.get("audio"));
        detail.put("global_fields", node.getProperties().get("globalFields"));
        detail.put("application_node_dict", context.get("application_node_dict"));
        return  detail;
    }

    @Override
    public void saveContext(JSONObject nodeDetail, WorkflowManage workflowManage) {
        this.context.put("question", nodeDetail.get("question"));
        this.context.put("run_time", nodeDetail.get("run_time"));
        this.context.put("document", nodeDetail.get("document_list"));
        this.context.put("image", nodeDetail.get("image_list"));
        this.context.put("audio", nodeDetail.get("audio_list"));
    }

}