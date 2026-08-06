package com.maxkb4j.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.core.assistant.ParameterExtractionAssistant;
import com.maxkb4j.core.langchain4j.AiServiceFactory;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.ModelConfig;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.ParameterExtractionNode;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NodeHandlerType(NodeType.PARAMETER_EXTRACTION)
@RequiredArgsConstructor
@Component
public class ParameterExtractionNodeHandler extends AbsNodeHandler {

    private final IModelProviderService modelFactory;


    @Override
    protected NodeResult doExecute(IWorkflow workflow, AbsNode node) throws Exception {
        ParameterExtractionNode.NodeParams params = parseParams(node, ParameterExtractionNode.NodeParams.class);
        ModelConfig modelConfig = resolveModelConfig(workflow, params);
        ChatModel chatModel = modelFactory.buildChatModel(modelConfig.getModelId(), modelConfig.getModelParamsSetting());
        Object request = workflow.getReferenceField(params.getInputVariable());

        ParameterExtractionAssistant assistant = AiServiceFactory.builder(ParameterExtractionAssistant.class)
                .chatModel(chatModel)
                .build();

        String extractInfo = format(params.getVariableList());
        Result<Map<String, Object>> result = assistant.extract(extractInfo, String.valueOf(request));

        putDetail(node, "request", request);
        recordTokenUsage(node, result.tokenUsage());

        Map<String, Object> nodeVariable = new HashMap<>();
        Map<String, Object> arguments = result.content();
        nodeVariable.put("result", new JSONObject(arguments));
        nodeVariable.putAll(arguments);
        return new NodeResult(nodeVariable);
    }

    protected String format(List<ParameterExtractionNode.Field> fields) {
        StringBuilder textBuilder = new StringBuilder();
        for (ParameterExtractionNode.Field field : fields) {
            textBuilder.append("\n");
            textBuilder.append("- ").append(field.getField()).append("(").append(field.getLabel()).append(")");
        }
        return textBuilder.toString();
    }
}
