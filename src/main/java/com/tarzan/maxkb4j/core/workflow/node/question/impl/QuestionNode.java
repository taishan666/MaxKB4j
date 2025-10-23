package com.tarzan.maxkb4j.core.workflow.node.question.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import lombok.Data;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.QUESTION;

public class QuestionNode extends INode {


    public QuestionNode(JSONObject properties) {
        super(properties);
        super.setType(QUESTION.getKey());
    }



    @Override
    public void saveContext(Workflow workflow, JSONObject detail) {
        context.put("answer", detail.get("answer"));
    }

    @Data
    public static class NodeParams {
        private String modelId;
        private String system;
        private String prompt;
        private Integer dialogueNumber;
        private JSONObject modelParamsSetting;
    }


}
