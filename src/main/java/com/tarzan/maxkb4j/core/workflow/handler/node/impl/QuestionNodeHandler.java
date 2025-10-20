package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.tarzan.maxkb4j.core.assistant.Assistant;
import com.tarzan.maxkb4j.core.langchain4j.AppChatMemory;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import com.tarzan.maxkb4j.core.workflow.enums.DialogueType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.node.question.input.QuestionParams;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import com.tarzan.maxkb4j.module.model.provider.service.impl.BaseChatModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@AllArgsConstructor
@Component
public class QuestionNodeHandler implements INodeHandler {

    private final ModelFactory modelFactory;
    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        QuestionParams nodeParams = node.getNodeData().toJavaObject(QuestionParams.class);
        BaseChatModel chatModel = modelFactory.build(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        List<ChatMessage> historyMessages=workflow.getHistoryMessages(nodeParams.getDialogueNumber(), DialogueType.WORKFLOW.name(), node.getRuntimeNodeId());
        node.getDetail().put("history_message", node.resetMessageList(historyMessages));
        String question = workflow.generatePrompt(nodeParams.getPrompt());
        String systemPrompt = workflow.generatePrompt(nodeParams.getSystem());
        Assistant assistant = AiServices.builder(Assistant.class)
                .systemMessageProvider(chatMemoryId -> systemPrompt)
                .chatMemory(AppChatMemory.withMessages(historyMessages))
                .chatModel(chatModel.getChatModel())
                .build();
        Result<String> result = assistant.chat(question);
        node.getDetail().put("system", systemPrompt);
        node.getDetail().put("question", question);
        TokenUsage tokenUsage =  result.tokenUsage();
        node.getDetail().put("messageTokens", tokenUsage.inputTokenCount());
        node.getDetail().put("answerTokens", tokenUsage.outputTokenCount());
        node.setAnswerText(result.content());
        return new NodeResult(Map.of(
                "answer", node.getAnswerText()
        ), Map.of());
    }
}
