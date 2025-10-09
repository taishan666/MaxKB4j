package com.tarzan.maxkb4j.core.workflow.node.speechtotext.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.core.workflow.model.ChatFile;
import com.tarzan.maxkb4j.core.workflow.node.speechtotext.input.SpeechToTextParams;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseSpeechToText;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import com.tarzan.maxkb4j.common.util.SpringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.SPEECH_TO_TEXT;

public class SpeechToTextNode extends INode {

    private final ModelService modelService;
    private final MongoFileService fileService;

    public SpeechToTextNode(JSONObject properties) {
        super(properties);
        this.type = SPEECH_TO_TEXT.getKey();
        this.modelService = SpringUtil.getBean(ModelService.class);
        this.fileService = SpringUtil.getBean(MongoFileService.class);
    }



    @Override
    public NodeResult execute() {
        SpeechToTextParams nodeParams=super.getNodeData().toJavaObject(SpeechToTextParams.class);
        List<String> audioList = nodeParams.getAudioList();
        Object res = super.getReferenceField(audioList.get(0), audioList.get(1));
        BaseSpeechToText sttModel = modelService.getModelById(nodeParams.getSttModelId());
        @SuppressWarnings("unchecked")
        List<ChatFile> audioFiles = (List<ChatFile>) res;
        List<String> content = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (ChatFile file: audioFiles) {
            byte[] data = fileService.getBytes(file.getFileId());
            String suffix=file.getName().substring(file.getName().lastIndexOf(".") + 1);
            String result = sttModel.speechToText(data,suffix);
            sb.append(result);
            content.add("### combined_audio.mp3\n"+result);
        }
        String answer=sb.toString();
        detail.put("content", content);
        detail.put("audioList", audioFiles);
        return new NodeResult(Map.of("answer", answer,"result", answer), Map.of());
    }

    @Override
    public void saveContext(JSONObject detail) {
        context.put("result", detail.get("result"));
    }

    @Override
    public JSONObject getRunDetail() {
        return detail;
    }

}
