package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.common.util.MessageConverter;
import com.maxkb4j.core.assistant.Assistant;
import com.maxkb4j.core.langchain4j.AiChatMemory;
import com.maxkb4j.core.langchain4j.AiServiceFactory;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.DialogueType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.ModelConfig;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.QuestionNode;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@NodeHandlerType(NodeType.QUESTION)
@RequiredArgsConstructor
@Component
public class QuestionNodeHandler extends AbsNodeHandler {

    private final IModelProviderService modelFactory;

    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node) throws Exception {
        QuestionNode.NodeParams params = parseParams(node, QuestionNode.NodeParams.class);
        ModelConfig modelConfig = resolveModelConfig(workflow, params);
        ChatModel chatModel = modelFactory.buildChatModel(modelConfig.getModelId(), modelConfig.getModelParamsSetting());
        List<ChatMessage> historyMessages = workflow.getHistoryMessages(params.getDialogueNumber(), DialogueType.WORK_FLOW.name(), node.getRuntimeNodeId());

        putDetail(node, "historyMessage", MessageConverter.formatHistoryMessages(historyMessages));

        String question = workflow.renderPrompt(params.getPrompt());
        String systemPrompt = workflow.renderPrompt(params.getSystem());

        Assistant assistant = AiServiceFactory.builder(Assistant.class)
                .systemMessage(systemPrompt)
                .chatMemory(AiChatMemory.withMessages(historyMessages))
                .chatModel(chatModel)
                .build();

        Result<String> result = assistant.chat(question);

        // 使用辅助方法批量写入详情
        putDetails(node, Map.of(
                "system", systemPrompt,
                "question", question
        ));

        recordTokenUsage(node, result.tokenUsage());

        if (params.getIsResult()) {
            setAnswerText(node, result.content());
        }

        return new NodeResult(Map.of("answer", result.content()));
    }
}
