package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.tarzan.maxkb4j.core.assistant.Assistant;
import com.tarzan.maxkb4j.core.langchain4j.AppChatMemory;
import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.DialogueType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.QuestionNode;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
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

    private final ModelFactory modelFactory;
    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        QuestionNode.NodeParams nodeParams = node.getNodeData().toJavaObject(QuestionNode.NodeParams.class);
        ChatModel chatModel = modelFactory.buildChatModel(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        List<ChatMessage> historyMessages=workflow.getHistoryMessages(nodeParams.getDialogueNumber(), DialogueType.WORK_FLOW.name(), node.getRuntimeNodeId());
        node.getDetail().put("history_message", node.resetMessageList(historyMessages));
        String question = workflow.generatePrompt(nodeParams.getPrompt());
        String systemPrompt = workflow.generatePrompt(nodeParams.getSystem());
        Assistant assistant = AiServices.builder(Assistant.class)
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
