package com.tarzan.maxkb4j.module.application.workflow.node.questionnode.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.WorkflowManage;
import com.tarzan.maxkb4j.module.application.workflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.workflow.node.questionnode.IQuestionNode;
import com.tarzan.maxkb4j.module.application.workflow.node.questionnode.dto.QuestionParams;

import java.util.Objects;

public class BaseQuestionNode extends IQuestionNode {
    @Override
    public NodeResult execute(QuestionParams nodeParams, FlowParams workflowParams) {
        if (Objects.isNull(nodeParams.getModelParamsSetting())) {
            nodeParams.setModelParamsSetting(getDefaultModelParamsSetting(nodeParams.getModelId()));
        }
        //CompressingQueryTransformer queryTransformer=new CompressingQueryTransformer();
        return null;
    }

    private JSONObject getDefaultModelParamsSetting(String modelId) {
        // ModelEntity model = modelService.getCacheById(modelId);
        return new JSONObject();
    }


    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("system", context.get("system"));
        detail.put("question", context.get("question"));
        detail.put("history_message", context.get("history_message"));
        detail.put("answer", context.get("answer"));
        return detail;
    }

    @Override
    public void saveContext(JSONObject nodeDetail, WorkflowManage workflowManage) {

    }
}
