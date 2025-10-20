package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.ChatFile;
import com.tarzan.maxkb4j.core.workflow.node.texttospeech.input.TextToSpeechParams;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import com.tarzan.maxkb4j.module.model.provider.service.impl.BaseTextToSpeech;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
@Component
public class TextToSpeechNodeHandler implements INodeHandler {

    private final MongoFileService fileService;
    private final ModelFactory modelFactory;
    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        TextToSpeechParams nodeParams=node.getNodeData().toJavaObject(TextToSpeechParams.class);
        List<String> contentList=nodeParams.getContentList();
        Object content=workflow.getReferenceField(contentList.get(0),contentList.get(1));
        BaseTextToSpeech ttsModel = modelFactory.build(nodeParams.getTtsModelId(), nodeParams.getModelParamsSetting());
        byte[]  audioData = ttsModel.textToSpeech(content.toString());
        ChatFile fileVO = fileService.uploadFile("generated_audio_"+ UUID.randomUUID() +".mp3",audioData);
        // 使用字符串拼接生成 HTML 音频标签
        String audioLabel = "<audio src=\"" + fileVO.getUrl() + "\" controls style=\"width: 300px; height: 43px\"></audio>";
        node.getDetail().put("content",content);
        // 输出生成的 HTML 标签
        return new NodeResult(Map.of("answer",audioLabel,"result",List.of(fileVO)),Map.of());
    }
}
