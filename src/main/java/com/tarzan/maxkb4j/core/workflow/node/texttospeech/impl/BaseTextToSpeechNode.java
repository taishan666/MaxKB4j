package com.tarzan.maxkb4j.core.workflow.node.texttospeech.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.domain.ChatFile;
import com.tarzan.maxkb4j.core.workflow.node.texttospeech.input.TextToSpeechParams;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseTextToSpeech;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import com.tarzan.maxkb4j.util.SpringUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.TEXT_TO_SPEECH;

public class BaseTextToSpeechNode extends INode {

    private final MongoFileService fileService;
    private final ModelService modelService;

    public BaseTextToSpeechNode(JSONObject properties) {
        super(properties);
        this.type = TEXT_TO_SPEECH.getKey();
        this.fileService = SpringUtil.getBean(MongoFileService.class);
        this.modelService = SpringUtil.getBean(ModelService.class);
    }

    @Override
    public NodeResult execute() {
        System.out.println(TEXT_TO_SPEECH);
        TextToSpeechParams nodeParams=super.nodeParams.toJavaObject(TextToSpeechParams.class);
        List<String> contentList=nodeParams.getContentList();
        Object content=super.getWorkflowManage().getReferenceField(contentList.get(0),contentList.subList(1, contentList.size()));
        BaseTextToSpeech ttsModel = modelService.getModelById(nodeParams.getTtsModelId(), nodeParams.getModelParamsSetting());
        byte[]  audioData = ttsModel.textToSpeech(content.toString());
        ChatFile fileVO = fileService.uploadFile("generated_audio_"+ UUID.randomUUID() +".mp3",audioData);
        // 使用字符串拼接生成 HTML 音频标签
        String audioLabel = "<audio src=\"" + fileVO.getUrl() + "\" controls style=\"width: 300px; height: 43px\"></audio>";
        // 输出生成的 HTML 标签
        return new NodeResult(Map.of("content",content,"answer",audioLabel,"audioList",List.of(fileVO)),Map.of());
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("content",context.get("content"));
        detail.put("answer",context.get("answer"));
        return detail;
    }

}
