package com.tarzan.maxkb4j.core.workflow.node.texttospeech.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.core.workflow.model.ChatFile;
import com.tarzan.maxkb4j.core.workflow.node.texttospeech.input.TextToSpeechParams;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import com.tarzan.maxkb4j.module.model.provider.service.impl.BaseTextToSpeech;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import com.tarzan.maxkb4j.common.util.SpringUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.TEXT_TO_SPEECH;

public class TextToSpeechNode extends INode {


    public TextToSpeechNode(JSONObject properties) {
        super(properties);
        this.setType(TEXT_TO_SPEECH.getKey());
    }



    @Override
    public void saveContext(JSONObject detail) {
        context.put("result", detail.get("result"));
    }


}
