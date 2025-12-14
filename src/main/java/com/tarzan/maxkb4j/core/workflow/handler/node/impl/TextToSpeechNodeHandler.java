package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.ChatFile;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.node.impl.TextToSpeechNode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.model.custom.base.TTSModel;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@NodeHandlerType(NodeType.TEXT_TO_SPEECH)
@RequiredArgsConstructor
@Component
public class TextToSpeechNodeHandler implements INodeHandler {

    private final MongoFileService fileService;
    private final ModelFactory modelFactory;

    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        TextToSpeechNode.NodeParams nodeParams=node.getNodeData().toJavaObject(TextToSpeechNode.NodeParams.class);
        List<String> contentList=nodeParams.getContentList();
        Object content=workflow.getReferenceField(contentList.get(0),contentList.get(1));
        TTSModel ttsModel = modelFactory.buildTTSModel(nodeParams.getTtsModelId(), nodeParams.getModelParamsSetting());
        byte[]  audioData = ttsModel.textToSpeech(content.toString());
        ChatFile fileVO = fileService.uploadFile("generated_audio_"+ UUID.randomUUID() +".mp3",audioData);
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
