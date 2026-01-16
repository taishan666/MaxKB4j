package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import lombok.Data;

import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.IMAGE_GENERATE;

public class ImageGenerateNode extends AbsNode {


    public ImageGenerateNode(String id,JSONObject properties) {
        super(id,properties);
        super.setType(IMAGE_GENERATE.getKey());
    }


    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        context.put("answer", detail.get("answer"));
        context.put("image", detail.get("image"));
    }

    @Data
    public static class NodeParams {
        private String modelId;
        private String prompt;
        private String negativePrompt;
        private Integer dialogueNumber;
        private String dialogueType;
        private JSONObject modelParamsSetting;
        private Boolean isResult;
    }


}
