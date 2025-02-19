package com.tarzan.maxkb4j.module.application.workflow.node.questionnode.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.WorkflowManage;
import com.tarzan.maxkb4j.module.application.workflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.workflow.node.questionnode.IQuestionNode;
import com.tarzan.maxkb4j.module.application.workflow.node.questionnode.dto.QuestionParams;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import com.tarzan.maxkb4j.module.model.service.ModelService;
import com.tarzan.maxkb4j.util.SpringUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.rag.query.Metadata;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;
import dev.langchain4j.rag.query.transformer.QueryTransformer;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.regex.Pattern;

public class BaseQuestionNode extends IQuestionNode {

    private final ModelService modelService;

    public BaseQuestionNode() {
        this.modelService = SpringUtil.getBean(ModelService.class);
    }

    @Override
    public NodeResult execute(QuestionParams nodeParams, FlowParams flowParams) {
        long startTime = System.currentTimeMillis();
        if (Objects.isNull(nodeParams.getModelParamsSetting())) {
            nodeParams.setModelParamsSetting(getDefaultModelParamsSetting(nodeParams.getModelId()));
        }
        System.out.println("execute耗时1 " + (System.currentTimeMillis() - startTime) + " ms");
        BaseChatModel chatModel = modelService.getModelById(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        System.out.println("execute耗时2 " + (System.currentTimeMillis() - startTime) + " ms");
        List<ChatMessage> historyMessage = getHistoryMessage(flowParams.getHistoryChatRecord(), nodeParams.getDialogueNumber());
        this.context.put("history_message", historyMessage);
        UserMessage question = super.workflowManage.generatePromptQuestion(nodeParams.getPrompt());
        this.context.put("question", question.singleText());
        String system = workflowManage.generatePrompt(nodeParams.getSystem());
        this.context.put("system", system);
        List<ChatMessage> messageList =  super.workflowManage.generateMessageList(system, question, historyMessage);
        QueryTransformer queryTransformer=new CompressingQueryTransformer(chatModel.getChatModel());
        Metadata metadata=new Metadata(question, flowParams.getChatId(), historyMessage);
        Query query=new Query(flowParams.getQuestion(),metadata);
        Collection<Query> list= queryTransformer.transform(query);
        StringBuilder answerSb=new StringBuilder();
        for (Query queryResult : list) {
            answerSb.append(queryResult.text());
        }
        Map<String, Object> nodeVariable = Map.of(
                "answer", answerSb.toString(),
                "chat_model", chatModel,
                "message_list", messageList,
                "history_message", historyMessage,
                "question", question.singleText()
        );
        context.put("message_tokens", 0);
        context.put("answer_tokens", 0);
        return new NodeResult(nodeVariable, Map.of());
    }
    public NodeResult execute1(QuestionParams nodeParams, FlowParams flowParams) {
        long startTime = System.currentTimeMillis();
        if (Objects.isNull(nodeParams.getModelParamsSetting())) {
            nodeParams.setModelParamsSetting(getDefaultModelParamsSetting(nodeParams.getModelId()));
        }
        System.out.println("execute耗时1 " + (System.currentTimeMillis() - startTime) + " ms");
        BaseChatModel chatModel = modelService.getModelById(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        System.out.println("execute耗时2 " + (System.currentTimeMillis() - startTime) + " ms");
        List<ChatMessage> historyMessage = getHistoryMessage(flowParams.getHistoryChatRecord(), nodeParams.getDialogueNumber());
        this.context.put("history_message", historyMessage);
        UserMessage question = super.workflowManage.generatePromptQuestion(nodeParams.getPrompt());
        this.context.put("question", question.singleText());
        String system = workflowManage.generatePrompt(nodeParams.getSystem());
        this.context.put("system", system);
        List<ChatMessage> messageList =  super.workflowManage.generateMessageList(system, question, historyMessage);
        ChatResponse res = chatModel.generate(messageList);
        Map<String, Object> nodeVariable = Map.of(
                "answer", res.aiMessage().text(),
                "chat_model", chatModel,
                "message_list", messageList,
                "history_message", historyMessage,
                "question", question.singleText()
        );
        TokenUsage tokenUsage = res.tokenUsage();
        context.put("message_tokens", tokenUsage.inputTokenCount());
        context.put("answer_tokens", tokenUsage.outputTokenCount());
        return new NodeResult(nodeVariable, Map.of());
    }

    public List<ChatMessage> getHistoryMessage(List<ApplicationChatRecordEntity> historyChatRecord, int dialogueNumber ) {
        List<ChatMessage> historyMessage = new ArrayList<>();
        int startIndex = Math.max(historyChatRecord.size() - dialogueNumber, 0);
        Pattern pattern = Pattern.compile("<form_rander>[\\s\\S]*?</form_rander>");
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

    public JSONArray resetMessageList(List<ChatMessage> historyMessage) {
        if (CollectionUtils.isEmpty(historyMessage)) {
            return new JSONArray();
        }
        JSONArray newMessageList = new JSONArray();
        for (ChatMessage chatMessage : historyMessage) {
            JSONObject message = new JSONObject();
            if (chatMessage instanceof SystemMessage systemMessage) {
                message.put("role", "ai");
                message.put("content", systemMessage.text());
            }
            if (chatMessage instanceof UserMessage userMessage) {
                message.put("role", "user");
                message.put("content", userMessage.singleText());
            }
            if (chatMessage instanceof AiMessage aiMessage) {
                message.put("role", "ai");
                message.put("content", aiMessage.text());
            }
            newMessageList.add(message);
        }
        return newMessageList;
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("system", context.get("system"));
        detail.put("question", context.get("question"));
        List<ChatMessage> historyMessage = (List<ChatMessage>) context.get("history_message");
        detail.put("history_message", resetMessageList(historyMessage));
        detail.put("answer", context.get("answer"));
        detail.put("message_tokens", context.get("message_tokens"));
        detail.put("answer_tokens", context.get("answer_tokens"));
        return detail;
    }

    @Override
    public void saveContext(JSONObject nodeDetail, WorkflowManage workflowManage) {

    }
}
