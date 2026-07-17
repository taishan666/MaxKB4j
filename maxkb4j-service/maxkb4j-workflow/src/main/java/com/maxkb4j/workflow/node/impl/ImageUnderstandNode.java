package com.maxkb4j.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.ModelAwareParams;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

import static com.maxkb4j.workflow.enums.NodeType.IMAGE_UNDERSTAND;


@Slf4j
public class ImageUnderstandNode extends AbsNode {

    public ImageUnderstandNode(String id,JSONObject properties) {
        super(id,properties);
        super.setType(IMAGE_UNDERSTAND.getKey());
    }

    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        context.put("answer", detail.get("answer"));
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
