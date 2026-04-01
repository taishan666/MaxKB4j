package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.common.domain.dto.OssFile;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.model.service.TTSModel;
import com.maxkb4j.oss.service.IOssService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbstractNodeHandler;
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
public class TextToSpeechNodeHandler extends AbstractNodeHandler {

    private final IOssService fileService;
    private final IModelProviderService modelFactory;

    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node) throws Exception {
        TextToSpeechNode.NodeParams params = parseParams(node, TextToSpeechNode.NodeParams.class);
        List<String> contentList = params.getContentList();
        Object content = workflow.getReferenceField(contentList);
        TTSModel ttsModel = modelFactory.buildTTSModel(params.getTtsModelId(), params.getModelParamsSetting());
        byte[] audioData = ttsModel.textToSpeech(content.toString());
        OssFile fileVO = fileService.uploadFile("generated_audio_" + UUID.randomUUID() + ".mp3", audioData);
        putDetail(node, "content", content);
        if (params.getIsResult()) {
            String answer = "<audio src=\"" + fileVO.getUrl() + "\" controls style=\"width: 300px; height: 43px\"></audio>";
            setAnswer(node, answer);
        }
        return new NodeResult(Map.of("result", List.of(fileVO)));
    }
}
