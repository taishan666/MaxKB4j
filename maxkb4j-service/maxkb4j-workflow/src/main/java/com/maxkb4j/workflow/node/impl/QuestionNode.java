package com.maxkb4j.workflow.node.impl;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.ModelAwareParams;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;

import java.util.List;
import java.util.Map;

@NodeCreatorType(NodeType.QUESTION)
public class QuestionNode extends AbsNode {

    public QuestionNode(String id,JSONObject properties) {
        super(id,properties);
    }

    @Override
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        context.put("answer", detail.get("answer"));
    }

    @Data
    public static class NodeParams implements ModelAwareParams {
        private String modelId;
        private String modelIdType;
        private List<String> modelIdReference;
        private String system;
        private String prompt;
        private Integer dialogueNumber;
        private JSONObject modelParamsSetting;
        private Boolean isResult;
    }

}
