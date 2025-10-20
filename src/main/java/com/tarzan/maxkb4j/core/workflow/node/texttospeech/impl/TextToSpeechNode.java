package com.tarzan.maxkb4j.core.workflow.node.texttospeech.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.TEXT_TO_SPEECH;

public class TextToSpeechNode extends INode {


    public TextToSpeechNode(JSONObject properties) {
        super(properties);
        this.setType(TEXT_TO_SPEECH.getKey());
    }



    @Override
    public void saveContext(Workflow workflow, JSONObject detail) {
        context.put("result", detail.get("result"));
    }


}
