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
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.rag.query.Metadata;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;
import dev.langchain4j.rag.query.transformer.QueryTransformer;

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
        List<ChatMessage> historyMessage = new ArrayList<>();
        String question = super.generatePrompt(nodeParams.getPrompt());
        UserMessage userMessage = UserMessage.from(question);
        String system = super.generatePrompt(nodeParams.getSystem());
        List<ChatMessage> messageList = super.generateMessageList(system, userMessage, historyMessage);
        QueryTransformer queryTransformer = new CompressingQueryTransformer(chatModel.getChatModel());
        Metadata metadata = new Metadata(userMessage, getChatParams().getChatId(), historyMessage);
        Query query = new Query(getChatParams().getMessage(), metadata);
        Collection<Query> list = queryTransformer.transform(query);
        StringBuilder answerSb = new StringBuilder();
        for (Query queryResult : list) {
            System.out.println(queryResult.metadata());
            answerSb.append(queryResult.text());
        }
        Map<String, Object> nodeVariable = Map.of(
                "answer", answerSb.toString(),
                "system", system,
                "message_list", messageList,
                "history_message", historyMessage,
                "question", question
        );
        context.put("messageTokens", TokenUtil.countTokens(messageList));
        context.put("answerTokens", TokenUtil.countTokens(answerSb.toString()));
        return new NodeResult(nodeVariable, Map.of());
    }


    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("system", context.get("system"));
        detail.put("question", context.get("question"));
        @SuppressWarnings("unchecked")
        List<ChatMessage> historyMessage = (List<ChatMessage>) context.get("history_message");
        detail.put("history_message", resetMessageList(historyMessage));
        detail.put("answer", context.get("answer"));
        detail.put("messageTokens", context.get("messageTokens"));
        detail.put("answerTokens", context.get("answerTokens"));
        return detail;
    }

}
