package com.tarzan.maxkb4j.core.workflow.node.speechtotext.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import lombok.Data;

import java.util.List;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.SPEECH_TO_TEXT;

public class SpeechToTextNode extends INode {


    public SpeechToTextNode(JSONObject properties) {
        super(properties);
        this.setType(SPEECH_TO_TEXT.getKey());
    }




    @Override
    public void saveContext(Workflow workflow, JSONObject detail) {
        context.put("result", detail.get("result"));
    }


    @Data
    public static class NodeParams {
        private String sttModelId;
        private List<String> audioList;

    }
}
