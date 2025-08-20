package com.tarzan.maxkb4j.core.workflow.node.speechtotext.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.domain.ChatFile;
import com.tarzan.maxkb4j.core.workflow.node.speechtotext.input.SpeechToTextParams;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseSpeechToText;
import com.tarzan.maxkb4j.module.resource.service.MongoFileService;
import com.tarzan.maxkb4j.util.SpringUtil;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.SPEECH_TO_TEXT;

public class BaseSpeechToTextNode extends INode {

    private final ModelService modelService;
    private final MongoFileService fileService;

    public BaseSpeechToTextNode() {
        super();
        this.type = SPEECH_TO_TEXT.getKey();
        this.modelService = SpringUtil.getBean(ModelService.class);
        this.fileService = SpringUtil.getBean(MongoFileService.class);
    }



    @Override
    public NodeResult execute() {
        SpeechToTextParams nodeParams=super.nodeParams.toJavaObject(SpeechToTextParams.class);
        List<String> audioList = nodeParams.getAudioList();
        Object res = super.getWorkflowManage().getReferenceField(audioList.get(0), audioList.subList(1, audioList.size()));
        BaseSpeechToText sttModel = modelService.getModelById(nodeParams.getSttModelId());
        List<ChatFile> audioFiles = (List<ChatFile>) res;
        StringBuilder sb = new StringBuilder();
        for (ChatFile file: audioFiles) {
            byte[] data = fileService.getBytes(file.getFileId());
            String suffix=file.getName().substring(file.getName().lastIndexOf(".") + 1);
            String result = sttModel.speechToText(data,suffix);
            sb.append(result);
        }
        return new NodeResult(Map.of("answer", sb.toString(), "result", sb.toString(), "audioList", audioList), Map.of());
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("content", context.get("content"));
        detail.put("answer", context.get("answer"));
        detail.put("audioList", context.get("audioList"));
        return detail;
    }

}
