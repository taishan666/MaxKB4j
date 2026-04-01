package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.common.domain.dto.MessageConverter;
import com.maxkb4j.core.assistant.Assistant;
import com.maxkb4j.core.langchain4j.AppChatMemory;
import com.maxkb4j.core.langchain4j.AssistantServices;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.DialogueType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbstractNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.QuestionNode;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.output.TokenUsage;
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
public class QuestionNodeHandler extends AbstractNodeHandler<QuestionNode.NodeParams> {

    private final IModelProviderService modelFactory;

    @Override
    protected Class<QuestionNode.NodeParams> getParamsClass() {
        return QuestionNode.NodeParams.class;
    }

    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node, QuestionNode.NodeParams params) throws Exception {
        ChatModel chatModel = modelFactory.buildChatModel(params.getModelId(), params.getModelParamsSetting());
        List<ChatMessage> historyMessages = workflow.getHistoryMessages(params.getDialogueNumber(), DialogueType.WORK_FLOW.name(), node.getRuntimeNodeId());

        putDetail(node, "history_message", MessageConverter.resetMessageList(historyMessages));

        String question = workflow.renderPrompt(params.getPrompt());
        String systemPrompt = workflow.renderPrompt(params.getSystem());

        Assistant assistant = AssistantServices.builder(Assistant.class)
                .systemMessage(systemPrompt)
                .chatMemory(AppChatMemory.withMessages(historyMessages))
                .chatModel(chatModel)
                .build();

        Result<String> result = assistant.chat(question);

        // 使用辅助方法批量写入详情
        putDetails(node, Map.of(
                "system", systemPrompt,
                "question", question
        ));

        TokenUsage tokenUsage = result.tokenUsage();
        if (tokenUsage != null) {
            putDetails(node, Map.of(
                    "messageTokens", tokenUsage.inputTokenCount(),
                    "answerTokens", tokenUsage.outputTokenCount()
            ));
        }

        if (params.getIsResult()) {
            setAnswer(node, result.content());
        }

        return new NodeResult(Map.of("answer", result.content()));
    }
}
