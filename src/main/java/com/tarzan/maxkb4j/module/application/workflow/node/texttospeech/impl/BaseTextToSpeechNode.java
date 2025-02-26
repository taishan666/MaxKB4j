package com.tarzan.maxkb4j.module.application.workflow.node.texttospeech.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.WorkflowManage;
import com.tarzan.maxkb4j.module.application.workflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.workflow.node.texttospeech.ITextToSpeechNode;
import com.tarzan.maxkb4j.module.application.workflow.node.texttospeech.dto.TextToSpeechParams;
import com.tarzan.maxkb4j.module.file.service.FileService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseTextToSpeech;
import com.tarzan.maxkb4j.module.model.service.ModelService;
import com.tarzan.maxkb4j.util.SpringUtil;

import java.util.List;
import java.util.Map;

public class BaseTextToSpeechNode extends ITextToSpeechNode {

    private final FileService fileService;
    private final ModelService modelService;

    public BaseTextToSpeechNode() {
        this.fileService = SpringUtil.getBean(FileService.class);
        this.modelService = SpringUtil.getBean(ModelService.class);
    }
    @Override
    public NodeResult execute(TextToSpeechParams nodeParams, FlowParams workflowParams) {
        List<String> contentList=nodeParams.getContentList();
        Object content=super.getWorkflowManage().getReferenceField(contentList.get(0),contentList.subList(1, contentList.size()));
        BaseTextToSpeech ttsModel = modelService.getModelById(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        byte[]  audioData = ttsModel.textToSpeech(content.toString());
        JSONObject fileVO = fileService.uploadFile("generated_audio.mp3",audioData);
        // 使用字符串拼接生成 HTML 音频标签
        String audioLabel = "<audio src=\"" + fileVO.getString("url") + "\" controls style=\"width: 300px; height: 43px\"></audio>";
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

    @Override
    public void saveContext(JSONObject nodeDetail, WorkflowManage workflowManage) {

    }
}
