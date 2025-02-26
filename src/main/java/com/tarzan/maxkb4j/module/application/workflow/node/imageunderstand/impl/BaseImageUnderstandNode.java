package com.tarzan.maxkb4j.module.application.workflow.node.imageunderstand.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.WorkflowManage;
import com.tarzan.maxkb4j.module.application.workflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.workflow.node.imageunderstand.IImageUnderstandNode;
import com.tarzan.maxkb4j.module.application.workflow.node.imageunderstand.dto.ImageUnderstandParams;

public class BaseImageUnderstandNode extends IImageUnderstandNode {
    @Override
    public NodeResult execute(ImageUnderstandParams nodeParams, FlowParams workflowParams) {
        return null;
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("answer",context.get("answer"));
        return detail;
    }

    @Override
    public void saveContext(JSONObject nodeDetail, WorkflowManage workflowManage) {

    }
}
