package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.TEXT_TO_SPEECH;

public class TextToSpeechNode extends INode {


    public TextToSpeechNode(JSONObject properties) {
        super(properties);
        this.setType(TEXT_TO_SPEECH.getKey());
    }



    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        context.put("result", detail.get("result"));
    }

    @Data
    public static class NodeParams {
        private String ttsModelId;
        private List<String> contentList;
        private JSONObject modelParamsSetting;
    }

}
