package com.maxkb4j.workflow.node.impl;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.maxkb4j.workflow.enums.NodeType.IMAGE_GENERATE;


@NodeCreatorType(NodeType.IMAGE_GENERATE)
public class ImageGenerateNode extends AbsNode {


    public ImageGenerateNode(String id,JSONObject properties) {
        super(id,properties);
        super.setType(IMAGE_GENERATE.getKey());
    }


    @Override
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        context.put("answer", detail.get("answer"));
        context.put("image", detail.get("image"));
    }

    @Data
    public static class NodeParams {
        private String modelId;
        private String modelIdType;
        private List<String> modelIdReference;
        private String prompt;
        private String negativePrompt;
        private Integer dialogueNumber;
        private String dialogueType;
        private JSONObject modelParamsSetting;
        private List<String> imageList;
        private Boolean isResult;
    }


}
