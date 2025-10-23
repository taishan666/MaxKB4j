package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.ChatFile;
import com.tarzan.maxkb4j.core.workflow.node.speechtotext.impl.SpeechToTextNode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import com.tarzan.maxkb4j.module.model.provider.service.impl.BaseSpeechToText;
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
        BaseSpeechToText sttModel = modelFactory.build(nodeParams.getSttModelId());
        @SuppressWarnings("unchecked")
        List<ChatFile> audioFiles = (List<ChatFile>) res;
        List<String> content = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (ChatFile file: audioFiles) {
            byte[] data = fileService.getBytes(file.getFileId());
            String suffix=file.getName().substring(file.getName().lastIndexOf(".") + 1);
            String result = sttModel.speechToText(data,suffix);
            sb.append(result);
            content.add("### combined_audio.mp3\n"+result);
        }
        String answer=sb.toString();
        node.getDetail().put("content", content);
        node.getDetail().put("audioList", audioFiles);
        return new NodeResult(Map.of("answer", answer,"result", answer), Map.of());
    }
}
