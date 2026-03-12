package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.common.domain.dto.MessageConverter;
import com.maxkb4j.core.assistant.Assistant;
import com.maxkb4j.core.langchain4j.AppChatMemory;
import com.maxkb4j.core.langchain4j.AssistantServices;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.DialogueType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.INodeHandler;
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
public class QuestionNodeHandler implements INodeHandler {

    private final IModelProviderService modelFactory;
    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        QuestionNode.NodeParams nodeParams = node.getNodeData().toJavaObject(QuestionNode.NodeParams.class);
        ChatModel chatModel = modelFactory.buildChatModel(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        List<ChatMessage> historyMessages=workflow.getHistoryMessages(nodeParams.getDialogueNumber(), DialogueType.WORK_FLOW.name(), node.getRuntimeNodeId());
        node.getDetail().put("history_message", MessageConverter.resetMessageList(historyMessages));
        String question = workflow.renderPrompt(nodeParams.getPrompt());
        String systemPrompt = workflow.renderPrompt(nodeParams.getSystem());
        Assistant assistant = AssistantServices.builder(Assistant.class)
                .systemMessageProvider(chatMemoryId -> systemPrompt)
                .chatMemory(AppChatMemory.withMessages(historyMessages))
                .chatModel(chatModel)
                .build();
        Result<String> result = assistant.chat(question);
        node.getDetail().put("system", systemPrompt);
        node.getDetail().put("question", question);
        TokenUsage tokenUsage =  result.tokenUsage();
        node.getDetail().put("messageTokens", tokenUsage.inputTokenCount());
        node.getDetail().put("answerTokens", tokenUsage.outputTokenCount());
        if (nodeParams.getIsResult()){
            node.setAnswerText(result.content());
        }
        return new NodeResult(Map.of(
                "answer", result.content()
        ));
    }
}
