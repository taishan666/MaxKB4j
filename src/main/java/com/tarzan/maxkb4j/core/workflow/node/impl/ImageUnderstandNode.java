package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.IMAGE_UNDERSTAND;

@Slf4j
public class ImageUnderstandNode extends INode {


    public ImageUnderstandNode(JSONObject properties) {
        super(properties);
        super.setType(IMAGE_UNDERSTAND.getKey());
    }


    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        context.put("answer", detail.get("answer"));
    }


    @Data
    public static class NodeParams  {
        private String modelId;
        private String system;
        private String prompt;
        private Integer dialogueNumber;
        private String dialogueType;
        private Boolean isResult;//流式输出
        private List<String> imageList;
        private JSONObject modelParamsSetting;

    }

}
