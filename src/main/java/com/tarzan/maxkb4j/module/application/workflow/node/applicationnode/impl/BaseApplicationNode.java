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
        JSONObject details = new JSONObject();
        details.put("index", index);
        details.put("name", node.getProperties().getString("stepName"));
        details.put("type", node.getType());
        details.put("run_time", context.getInteger("run_time"));
        details.put("status", status);
        details.put("err_message", errMessage);
        details.put("info", node.getProperties().getString("node_data"));
        details.put("question", context.getString("question"));
        details.put("answer", context.getString("answer"));
        details.put("message_tokens", context.getString("message_tokens"));
        details.put("answer_tokens", context.getString("answer_tokens"));
        details.put("image_list", context.get("image"));
        details.put("document_list", context.get("document"));
        details.put("audio_list", context.get("audio"));
        details.put("global_fields", node.getProperties().get("globalFields"));
        details.put("application_node_dict", context.get("application_node_dict"));
        return  details;
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