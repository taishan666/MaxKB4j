package com.tarzan.maxkb4j.core.workflow.node.question.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.common.util.SpringUtil;
import com.tarzan.maxkb4j.common.util.TokenUtil;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.node.question.input.QuestionParams;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.rag.query.Metadata;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;
import dev.langchain4j.rag.query.transformer.QueryTransformer;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.QUESTION;

public class QuestionNode extends INode {

    private final ModelService modelService;

    public QuestionNode(JSONObject properties) {
        super(properties);
        this.type = QUESTION.getKey();
        this.modelService = SpringUtil.getBean(ModelService.class);
    }


    @Override
    public NodeResult execute() {
        QuestionParams nodeParams = super.getNodeData().toJavaObject(QuestionParams.class);
        if (Objects.isNull(nodeParams.getModelParamsSetting())) {
            nodeParams.setModelParamsSetting(new JSONObject());
        }
        BaseChatModel chatModel = modelService.getModelById(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        List<ChatMessage> historyMessages=super.getHistoryMessages(nodeParams.getDialogueNumber(), "WORKFLOW", runtimeNodeId);
        String question = super.generatePrompt(nodeParams.getPrompt());
        UserMessage userMessage = UserMessage.from(question);
        String system = super.generatePrompt(nodeParams.getSystem());
        List<ChatMessage> messageList = this.generateMessageList(system, userMessage, historyMessages);
        QueryTransformer queryTransformer = new CompressingQueryTransformer(chatModel.getChatModel());
        Metadata metadata = new Metadata(userMessage, getChatParams().getChatId(), historyMessages);
        Query query = new Query(getChatParams().getMessage(), metadata);
        Collection<Query> list = queryTransformer.transform(query);
        StringBuilder answerSb = new StringBuilder();
        for (Query queryResult : list) {
            System.out.println(queryResult.metadata());
            answerSb.append(queryResult.text());
        }
        return new NodeResult(Map.of(
                "system", system,
                "question", question,
                "answer", answerSb.toString(),
                "history_message", resetMessageList(historyMessages),
                "messageTokens", TokenUtil.countTokens(messageList),
                "answerTokens",TokenUtil.countTokens(answerSb.toString())
        ), Map.of());
    }

    private List<ChatMessage> generateMessageList(String system, UserMessage question, List<ChatMessage> historyMessages) {
        List<ChatMessage> messageList = new ArrayList<>();
        if (StringUtils.isNotBlank(system)) {
            messageList.add(SystemMessage.from(system));
        }
        messageList.addAll(historyMessages);
        messageList.add(question);
        return messageList;
    }

    @Override
    public void saveContext(JSONObject detail) {
        context.put("system", detail.get("system"));
        context.put("question", detail.get("question"));
        context.put("answer", detail.get("answer"));
        context.put("history_message", detail.get("history_message"));
        context.put("messageTokens", detail.get("messageTokens"));
        context.put("answerTokens", detail.get("answerTokens"));
    }


    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("system", context.get("system"));
        detail.put("question", context.get("question"));
        detail.put("history_message", context.get("history_message"));
        detail.put("answer", context.get("answer"));
        detail.put("messageTokens", context.get("messageTokens"));
        detail.put("answerTokens", context.get("answerTokens"));
        return detail;
    }

}
