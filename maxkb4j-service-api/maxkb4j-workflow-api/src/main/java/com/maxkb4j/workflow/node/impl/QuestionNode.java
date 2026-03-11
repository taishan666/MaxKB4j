package com.maxkb4j.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;

import java.util.Map;

import static com.maxkb4j.workflow.enums.NodeType.QUESTION;

public class QuestionNode extends AbsNode {


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
