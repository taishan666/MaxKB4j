package com.maxkb4j.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.OssFile;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.model.service.ITTSModel;
import com.maxkb4j.oss.service.IOssService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.ModelConfig;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.TextToSpeechNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@NodeHandlerType(NodeType.TEXT_TO_SPEECH)
@RequiredArgsConstructor
@Component
public class TextToSpeechNodeHandler extends AbsNodeHandler {

    private final IOssService ossService;
    private final IModelProviderService modelFactory;

    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node) throws Exception {
        TextToSpeechNode.NodeParams params = parseParams(node, TextToSpeechNode.NodeParams.class);
        String modelId = params.getTtsModelId();
        JSONObject modelParamsSetting = params.getModelParamsSetting();
        if (params.getModelIdType() != null && params.getModelIdType().equals("reference")){
            ModelConfig modelConfig = (ModelConfig) workflow.getReferenceField(params.getModelIdReference());
            modelId = modelConfig.getModelId();
            modelParamsSetting = modelConfig.getModelParamsSetting();
        }
        ITTSModel ttsModel = modelFactory.buildTTSModel(modelId, modelParamsSetting);
        List<String> contentList = params.getContentList();
        Object content = workflow.getReferenceField(contentList);
        byte[] audioData = ttsModel.textToSpeech(content.toString());
        OssFile ossFile = ossService.uploadFile("generated_audio_" + UUID.randomUUID() + ".mp3", audioData);
        putDetail(node, "content", content);
        if (params.getIsResult()) {
            String answer = "<audio src=\"" + ossFile.getUrl() + "\" controls style=\"width: 300px; height: 43px\"></audio>";
            setAnswer(node, answer);
        }
        return new NodeResult(Map.of("result", List.of(ossFile)));
    }
}
