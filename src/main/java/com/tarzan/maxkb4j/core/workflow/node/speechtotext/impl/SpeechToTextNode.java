package com.tarzan.maxkb4j.core.workflow.node.speechtotext.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.core.workflow.model.ChatFile;
import com.tarzan.maxkb4j.core.workflow.node.speechtotext.input.SpeechToTextParams;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import com.tarzan.maxkb4j.module.model.provider.service.impl.BaseSpeechToText;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import com.tarzan.maxkb4j.common.util.SpringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.SPEECH_TO_TEXT;

public class SpeechToTextNode extends INode {


    public SpeechToTextNode(JSONObject properties) {
        super(properties);
        this.setType(SPEECH_TO_TEXT.getKey());
    }




    @Override
    public void saveContext(JSONObject detail) {
        context.put("result", detail.get("result"));
    }


}
