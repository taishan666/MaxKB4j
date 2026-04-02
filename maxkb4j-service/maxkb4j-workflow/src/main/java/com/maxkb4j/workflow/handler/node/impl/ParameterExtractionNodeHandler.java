package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.core.assistant.ParameterExtractionAssistant;
import com.maxkb4j.core.langchain4j.AssistantServices;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.ParameterExtractionNode;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.output.TokenUsage;
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
    protected NodeResult doExecute(Workflow workflow, AbsNode node) throws Exception {
        ParameterExtractionNode.NodeParams params = parseParams(node, ParameterExtractionNode.NodeParams.class);
        ChatModel chatModel = modelFactory.buildChatModel(params.getModelId(), params.getModelParamsSetting());
        Object query = workflow.getReferenceField(params.getInputVariable());

        ParameterExtractionAssistant assistant = AssistantServices.builder(ParameterExtractionAssistant.class)
                .chatModel(chatModel)
                .build();

        String extractInfo = format(params.getVariableList());
        Result<Map<String, Object>> result = assistant.extract(extractInfo, query.toString());

        TokenUsage tokenUsage = result.tokenUsage();
        putDetails(node, Map.of(
                "question", query,
                "messageTokens", tokenUsage.inputTokenCount(),
                "answerTokens", tokenUsage.outputTokenCount()
        ));

        Map<String, Object> nodeVariable = new HashMap<>();
        nodeVariable.put("result", result.content());
        nodeVariable.putAll(result.content());

        return new NodeResult(nodeVariable);
    }

    protected String format(List<ParameterExtractionNode.Field> fields) {
        StringBuilder textBuilder = new StringBuilder();
        for (ParameterExtractionNode.Field field : fields) {
            textBuilder.append("\n");
            textBuilder.append("- ").append(field.getLabel()).append("(").append(field.getField()).append(")");
        }
        return textBuilder.toString();
    }
}
