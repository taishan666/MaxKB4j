package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.common.domain.dto.OssFile;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.model.service.STTModel;
import com.maxkb4j.oss.service.IOssService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbstractNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.SpeechToTextNode;
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
public class SpeechToTextNodeHandler extends AbstractNodeHandler {

    private final IModelProviderService modelFactory;
    private final IOssService fileService;

    @SuppressWarnings("unchecked")
    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node) throws Exception {
        SpeechToTextNode.NodeParams params = parseParams(node, SpeechToTextNode.NodeParams.class);
        List<String> audioList = params.getAudioList();
        Object res = workflow.getReferenceField(audioList);
        STTModel sttModel = modelFactory.buildSTTModel(params.getSttModelId());
        List<OssFile> audioFiles = (List<OssFile>) res;

        List<String> content = new ArrayList<>();
        List<String> answerTextList = new ArrayList<>();

        for (OssFile file : audioFiles) {
            byte[] data = fileService.getBytes(file.getFileId());
            String suffix = file.getName().substring(file.getName().lastIndexOf(".") + 1);
            String result = sttModel.speechToText(data, suffix);
            answerTextList.add(result);
            content.add("### " + file.getName() + "\n" + result);
        }

        String answer = String.join("\n", answerTextList);

        putDetails(node, Map.of(
                "content", content,
                "audioList", audioFiles
        ));

        if (params.getIsResult()) {
            setAnswer(node, answer);
        }

        return new NodeResult(Map.of("result", answer));
    }
}
