package com.tarzan.maxkb4j.module.application.workflow.node.speechToTextNode.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.WorkflowManage;
import com.tarzan.maxkb4j.module.application.workflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.workflow.node.speechToTextNode.ISpeechToTextNode;
import com.tarzan.maxkb4j.module.application.workflow.node.speechToTextNode.dto.SpeechToTextParams;
import com.tarzan.maxkb4j.module.file.service.FileService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseSpeechToText;
import com.tarzan.maxkb4j.module.model.service.ModelService;
import com.tarzan.maxkb4j.util.SpringUtil;

import java.util.List;
import java.util.Map;

public class BaseSpeechToTextNode extends ISpeechToTextNode {

    private final ModelService modelService;
    private final FileService fileService;

    public BaseSpeechToTextNode() {
        this.modelService = SpringUtil.getBean(ModelService.class);
        this.fileService = SpringUtil.getBean(FileService.class);
    }

    @Override
    public NodeResult execute(SpeechToTextParams nodeParams, FlowParams workflowParams) {
        List<String> audioList = nodeParams.getAudioList();
        Object res = super.getWorkflowManage().getReferenceField(audioList.get(0), audioList.subList(1, audioList.size()));
        BaseSpeechToText sttModel = modelService.getModelById(nodeParams.getSttModelId());
        List<JSONObject> audioFiles = (List<JSONObject>) res;
        StringBuilder sb = new StringBuilder();
        for (JSONObject file: audioFiles) {
            byte[] data = fileService.getBytes(file.getString("file_id"));
            String result = sttModel.speechToText(data);
            sb.append(result);
        }
        return new NodeResult(Map.of("answer", sb.toString(), "result", sb.toString(), "audio_list", audioList), Map.of());
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("content", context.get("content"));
        detail.put("answer", context.get("answer"));
        detail.put("audio_list", context.get("audio_list"));
        return detail;
    }

    @Override
    public void saveContext(JSONObject nodeDetail, WorkflowManage workflowManage) {

    }
}
