package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.INodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.TextToSpeechNode;
import com.maxkb4j.model.service.TTSModel;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.common.domain.dto.OssFile;
import com.maxkb4j.oss.service.IOssService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@NodeHandlerType(NodeType.TEXT_TO_SPEECH)
@RequiredArgsConstructor
@Component
public class TextToSpeechNodeHandler implements INodeHandler {

    private final IOssService fileService;
    private final IModelProviderService modelFactory;

    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        TextToSpeechNode.NodeParams nodeParams=node.getNodeData().toJavaObject(TextToSpeechNode.NodeParams.class);
        List<String> contentList=nodeParams.getContentList();
        Object content=workflow.getReferenceField(contentList);
        TTSModel ttsModel = modelFactory.buildTTSModel(nodeParams.getTtsModelId(), nodeParams.getModelParamsSetting());
        byte[]  audioData = ttsModel.textToSpeech(content.toString());
        OssFile fileVO = fileService.uploadFile("generated_audio_"+ UUID.randomUUID() +".mp3",audioData);
        node.getDetail().put("content",content);
        if (nodeParams.getIsResult()){
            // 使用字符串拼接生成 HTML 音频标签
            String answer = "<audio src=\"" + fileVO.getUrl() + "\" controls style=\"width: 300px; height: 43px\"></audio>";
            node.setAnswerText(answer);
        }
        // 输出生成的 HTML 标签
        return new NodeResult(Map.of("result",List.of(fileVO)));
    }
}
