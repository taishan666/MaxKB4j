package com.maxkb4j.workflow.node.impl;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.ModelAwareParams;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
@NodeCreatorType(NodeType.IMAGE_UNDERSTAND)
public class ImageUnderstandNode extends AbsNode {

    public ImageUnderstandNode(String id,JSONObject properties) {
        super(id,properties);
    }

    @Override
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        Object answer = detail.get("answer");
        context.put("answer", answer);
        super.setAnswerText((String) answer);
        context.put("reasoningContent", detail.get("reasoningContent"));
    }

    @Data
    public static class NodeParams implements ModelAwareParams {
        private String modelId;
        private String modelIdType;
        private List<String> modelIdReference;
        private String system;
        private String prompt;
        private String dialogueType;
        private int dialogueNumber;
        private Boolean isResult;
        private JSONObject modelParamsSetting;
        private JSONObject modelSetting;
        private List<String> imageList;

    }

}
