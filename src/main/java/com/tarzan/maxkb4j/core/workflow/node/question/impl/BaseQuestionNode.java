package com.tarzan.maxkb4j.core.workflow.node.question.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.question.input.QuestionParams;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import com.tarzan.maxkb4j.util.SpringUtil;
import com.tarzan.maxkb4j.util.TokenUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.rag.query.Metadata;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;
import dev.langchain4j.rag.query.transformer.QueryTransformer;

import java.util.*;
import java.util.regex.Pattern;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.QUESTION;

public class BaseQuestionNode extends INode {

    private final ModelService modelService;

    public BaseQuestionNode() {
        this.type = QUESTION.getKey();
        this.modelService = SpringUtil.getBean(ModelService.class);
    }


    @Override
    public NodeResult execute() {
        QuestionParams nodeParams=super.nodeParams.toJavaObject(QuestionParams.class);
        if (Objects.isNull(nodeParams.getModelParamsSetting())) {
            nodeParams.setModelParamsSetting(getDefaultModelParamsSetting(nodeParams.getModelId()));
        }
        BaseChatModel chatModel = modelService.getModelById(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        List<ChatMessage> historyMessage = getHistoryMessage(super.flowParams.getHistoryChatRecord(), nodeParams.getDialogueNumber());
        UserMessage question = super.workflowManage.generatePromptQuestion(nodeParams.getPrompt());
        String system = workflowManage.generatePrompt(nodeParams.getSystem());
        List<ChatMessage> messageList =  super.workflowManage.generateMessageList(system, question, historyMessage);
        QueryTransformer queryTransformer=new CompressingQueryTransformer(chatModel.getChatModel());
        Metadata metadata=new Metadata(question, super.flowParams.getChatId(), historyMessage);
        Query query=new Query(super.flowParams.getQuestion(),metadata);
        Collection<Query> list= queryTransformer.transform(query);
        StringBuilder answerSb=new StringBuilder();
        for (Query queryResult : list) {
            System.out.println(queryResult.metadata());
            answerSb.append(queryResult.text());
        }
        Map<String, Object> nodeVariable = Map.of(
                "answer", answerSb.toString(),
                "chat_model", chatModel,
                "system",system,
                "message_list", messageList,
                "history_message", historyMessage,
                "question", question.singleText()
        );
        context.put("messageTokens", TokenUtil.countTokens(messageList));
        context.put("answerTokens", TokenUtil.countTokens(answerSb.toString()));
        return new NodeResult(nodeVariable, Map.of());
    }



    public List<ChatMessage> getHistoryMessage(List<ApplicationChatRecordEntity> historyChatRecord, int dialogueNumber ) {
        List<ChatMessage> historyMessage = new ArrayList<>();
        int startIndex = Math.max(historyChatRecord.size() - dialogueNumber, 0);
        Pattern pattern = Pattern.compile("<form_render>[\\s\\S]*?</form_render>");
        // 遍历指定范围内的聊天记录
        for (int index = startIndex; index < historyChatRecord.size(); index++) {
            String content = historyChatRecord.get(index).getProblemText();
            content = pattern.matcher(content).replaceAll("");
            // 获取每条消息并添加到历史消息列表中
            historyMessage.add(new UserMessage(content));
            historyMessage.add(new AiMessage(historyChatRecord.get(index).getAnswerText()));
        }
        // 使用Stream API和flatMap来代替Python中的reduce操作
        return historyMessage;
    }

    private JSONObject getDefaultModelParamsSetting(String modelId) {
        // ModelEntity model = modelService.getCacheById(modelId);
        return new JSONObject();
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("system", context.get("system"));
        detail.put("question", context.get("question"));
        List<ChatMessage> historyMessage = (List<ChatMessage>) context.get("history_message");
        detail.put("history_message", resetMessageList(historyMessage));
        detail.put("answer", context.get("answer"));
        detail.put("messageTokens", context.get("messageTokens"));
        detail.put("answerTokens", context.get("answerTokens"));
        return detail;
    }

}
