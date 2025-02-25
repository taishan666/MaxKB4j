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
      //  List<FileVO> audioFiles = (List<FileVO>) res;
        List<Map<String,Object>> audioFiles= (List<Map<String,Object>>) res;
        System.out.println(res);
        StringBuilder sb = new StringBuilder();
        for (Map<String,Object> map : audioFiles) {
            byte[] data = fileService.getBytes((String) map.get("file_id"));
            String result = sttModel.speechToText(data);
            sb.append(result);
        }
        return new NodeResult(Map.of("answer", sb.toString(), "result", "123"), Map.of());
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
