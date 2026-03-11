package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.INodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.SpeechToTextNode;
import com.maxkb4j.model.service.STTModel;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.oss.dto.SysFile;
import com.maxkb4j.oss.service.IOssService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Slf4j
@NodeHandlerType(NodeType.SPEECH_TO_TEXT)
@RequiredArgsConstructor
@Component
public class SpeechToTextNodeHandler implements INodeHandler {

    private final IModelProviderService modelFactory;
    private final IOssService fileService;

    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        SpeechToTextNode.NodeParams nodeParams=node.getNodeData().toJavaObject(SpeechToTextNode.NodeParams.class);
        List<String> audioList = nodeParams.getAudioList();
        Object res = workflow.getReferenceField(audioList);
        STTModel sttModel = modelFactory.buildSTTModel(nodeParams.getSttModelId());
        @SuppressWarnings("unchecked")
        List<SysFile> audioFiles = (List<SysFile>) res;
        List<String> content = new ArrayList<>();
        List<String> answerTextList = new ArrayList<>();
        for (SysFile file: audioFiles) {
            byte[] data = fileService.getBytes(file.getFileId());
            String suffix=file.getName().substring(file.getName().lastIndexOf(".") + 1);
            String result = sttModel.speechToText(data,suffix);
            answerTextList.add(result);
            content.add("### "+file.getName()+"\n"+result);
        }
        String answer= String.join("\n", answerTextList);
        node.getDetail().put("content", content);
        node.getDetail().put("audioList", audioFiles);
        if (nodeParams.getIsResult()){
            node.setAnswerText(answer);
        }
        return new NodeResult(Map.of("result", answer));
    }
}
