package com.tarzan.maxkb4j.core.workflow.node.question.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.common.util.SpringUtil;
import com.tarzan.maxkb4j.core.assistant.Assistant;
import com.tarzan.maxkb4j.core.langchain4j.AppChatMemory;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.enums.DialogueType;
import com.tarzan.maxkb4j.core.workflow.node.question.input.QuestionParams;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.QUESTION;

public class QuestionNode extends INode {

    private final ModelFactory modelFactory;

    public QuestionNode(JSONObject properties) {
        super(properties);
        super.setType(QUESTION.getKey());
        this.modelFactory = SpringUtil.getBean(ModelFactory.class);
    }

    @Override
    public NodeResult execute() {
        QuestionParams nodeParams = super.getNodeData().toJavaObject(QuestionParams.class);
        BaseChatModel chatModel = modelFactory.build(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        List<ChatMessage> historyMessages=super.getHistoryMessages(nodeParams.getDialogueNumber(), DialogueType.WORKFLOW.name(), super.getRuntimeNodeId());
        detail.put("history_message", resetMessageList(historyMessages));
        String question = super.generatePrompt(nodeParams.getPrompt());
        String systemPrompt = super.generatePrompt(nodeParams.getSystem());
        Assistant assistant = AiServices.builder(Assistant.class)
                .systemMessageProvider(chatMemoryId -> systemPrompt)
                .chatMemory(AppChatMemory.withMessages(historyMessages))
                .chatModel(chatModel.getChatModel())
                .build();
        Result<String> result = assistant.chat(question);
        detail.put("system", systemPrompt);
        detail.put("question", question);
        TokenUsage tokenUsage =  result.tokenUsage();
        detail.put("messageTokens", tokenUsage.inputTokenCount());
        detail.put("answerTokens", tokenUsage.outputTokenCount());
        super.setAnswerText(result.content());
        return new NodeResult(Map.of(
                "answer", super.getAnswerText()
        ), Map.of());
    }


    @Override
    public void saveContext(JSONObject detail) {
        context.put("answer", detail.get("answer"));
    }



}
