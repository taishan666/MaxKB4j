package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.ChatFile;
import com.tarzan.maxkb4j.core.workflow.node.impl.SpeechToTextNode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import com.tarzan.maxkb4j.module.model.custom.base.STTModel;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Slf4j
@AllArgsConstructor
@Component
public class SpeechToTextNodeHandler implements INodeHandler {

    private final ModelFactory modelFactory;
    private final MongoFileService fileService;

    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        SpeechToTextNode.NodeParams nodeParams=node.getNodeData().toJavaObject(SpeechToTextNode.NodeParams.class);
        List<String> audioList = nodeParams.getAudioList();
        Object res = workflow.getReferenceField(audioList.get(0), audioList.get(1));
        STTModel sttModel = modelFactory.buildSTTModel(nodeParams.getSttModelId());
        @SuppressWarnings("unchecked")
        List<ChatFile> audioFiles = (List<ChatFile>) res;
        List<String> content = new ArrayList<>();
        List<String> answerTextList = new ArrayList<>();
        for (ChatFile file: audioFiles) {
            byte[] data = fileService.getBytes(file.getFileId());
            String suffix=file.getName().substring(file.getName().lastIndexOf(".") + 1);
            String result = sttModel.speechToText(data,suffix);
            answerTextList.add(result);
            content.add("### "+file.getName()+"\n"+result);
        }
        String answer= String.join("\n", answerTextList);
        node.getDetail().put("content", content);
        node.getDetail().put("audioList", audioFiles);
        node.setAnswerText(answer);
        return new NodeResult(Map.of("result", answer), Map.of());
    }
}
