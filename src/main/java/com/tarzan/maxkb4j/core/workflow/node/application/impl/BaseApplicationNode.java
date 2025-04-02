package com.tarzan.maxkb4j.core.workflow.node.application.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.WorkflowManage;
import com.tarzan.maxkb4j.core.workflow.node.application.input.ApplicationNodeParams;

public class BaseApplicationNode extends INode {

    @Override
    public String getType() {
        return "application-node";
    }


    @Override
    public NodeResult execute() {
        ApplicationNodeParams nodeParams= super.nodeParams.toJavaObject(ApplicationNodeParams.class);
        //todo
        return null;
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("info", node.getProperties().getString("nodeData"));
        detail.put("question", context.get("question"));
        detail.put("answer", context.get("answer"));
        detail.put("messageTokens", context.get("messageTokens"));
        detail.put("answerTokens", context.get("answerTokens"));
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
        this.context.put("runTime", nodeDetail.get("runTime"));
        this.context.put("document", nodeDetail.get("document_list"));
        this.context.put("image", nodeDetail.get("image_list"));
        this.context.put("audio", nodeDetail.get("audio_list"));
    }

}