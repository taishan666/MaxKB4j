package com.maxkb4j.workflow.node.impl;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.ModelAwareParams;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;

import java.util.List;
import java.util.Map;

@NodeCreatorType(NodeType.TEXT_TO_SPEECH)
public class TextToSpeechNode extends AbsNode {

    public TextToSpeechNode(String id,JSONObject properties) {
        super(id,properties);
    }

    @Override
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        context.put("result", detail.get("result"));
    }

    @Data
    public static class NodeParams implements ModelAwareParams {
        private String ttsModelId;
        private String modelIdType;
        private List<String> modelIdReference;
        private List<String> contentList;
        private JSONObject modelParamsSetting;
        private Boolean isResult;

        @Override
        public String getModelId() {
            return ttsModelId;
        }
    }

}
