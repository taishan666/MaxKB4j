package com.tarzan.maxkb4j.core.workflow.node.texttospeech.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.dto.ChatFile;
import com.tarzan.maxkb4j.core.workflow.node.texttospeech.input.TextToSpeechParams;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseTextToSpeech;
import com.tarzan.maxkb4j.module.resource.service.FileService;
import com.tarzan.maxkb4j.util.SpringUtil;

import java.util.List;
import java.util.Map;

public class BaseTextToSpeechNode extends INode {

    private final FileService fileService;
    private final ModelService modelService;

    public BaseTextToSpeechNode() {
        this.fileService = SpringUtil.getBean(FileService.class);
        this.modelService = SpringUtil.getBean(ModelService.class);
    }

    @Override
    public String getType() {
        return "text-to-speech-node";
    }
    @Override
    public NodeResult execute() {
        TextToSpeechParams nodeParams=super.nodeParams.toJavaObject(TextToSpeechParams.class);
        List<String> contentList=nodeParams.getContentList();
        Object content=super.getWorkflowManage().getReferenceField(contentList.get(0),contentList.subList(1, contentList.size()));
        BaseTextToSpeech ttsModel = modelService.getModelById(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        byte[]  audioData = ttsModel.textToSpeech(content.toString());
        ChatFile fileVO = fileService.uploadFile("generated_audio.mp3",audioData);
        // 使用字符串拼接生成 HTML 音频标签
        String audioLabel = "<audio src=\"" + fileVO.getUrl() + "\" controls style=\"width: 300px; height: 43px\"></audio>";
        // 输出生成的 HTML 标签
        return new NodeResult(Map.of("answer",audioLabel,"content",content,"result",List.of(fileVO)),Map.of());
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("content",context.get("content"));
        detail.put("answer",context.get("answer"));
        return detail;
    }

}
