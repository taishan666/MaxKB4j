package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.tarzan.maxkb4j.core.assistant.ParameterExtractionAssistant;
import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.node.impl.ParameterExtractionNode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NodeHandlerType(NodeType.PARAMETER_EXTRACTION)
@RequiredArgsConstructor
@Component
public class ParameterExtractionNodeHandler implements INodeHandler {

    private final ModelFactory modelFactory;
    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        ParameterExtractionNode.NodeParams nodeParams = node.getNodeData().toJavaObject(ParameterExtractionNode.NodeParams.class);
        ChatModel chatModel = modelFactory.buildChatModel(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        Object query = workflow.getReferenceField(nodeParams.getInputVariable().get(0),nodeParams.getInputVariable().get(1));
        ParameterExtractionAssistant assistant = AiServices.builder(ParameterExtractionAssistant.class)
                .chatModel(chatModel)
                .build();
        String extractInfo=format(nodeParams.getVariableList());
        Result<Map<String, Object>> result = assistant.extract(extractInfo,query.toString());
        TokenUsage tokenUsage =  result.tokenUsage();
        node.getDetail().put("question", query);
        node.getDetail().put("messageTokens", tokenUsage.inputTokenCount());
        node.getDetail().put("answerTokens", tokenUsage.outputTokenCount());
        Map<String, Object> nodeVariable=new HashMap<>();
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
