package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import lombok.Data;

import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.QUESTION;

public class QuestionNode extends INode {


    public QuestionNode(String id,JSONObject properties) {
        super(id,properties);
        super.setType(QUESTION.getKey());
    }



    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        context.put("answer", detail.get("answer"));
    }

    @Data
    public static class NodeParams {
        private String modelId;
        private String system;
        private String prompt;
        private Integer dialogueNumber;
        private JSONObject modelParamsSetting;
        private Boolean isResult;
    }


}
