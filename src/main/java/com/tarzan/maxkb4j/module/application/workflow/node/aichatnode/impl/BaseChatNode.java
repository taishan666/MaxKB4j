package com.tarzan.maxkb4j.module.application.workflow.node.aichatnode.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.ChatStream;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.workflow.INode;
import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.WorkflowManage;
import com.tarzan.maxkb4j.module.application.workflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.workflow.node.aichatnode.IChatNode;
import com.tarzan.maxkb4j.module.application.workflow.node.aichatnode.dto.ChatNodeParams;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import com.tarzan.maxkb4j.module.model.service.ModelService;
import com.tarzan.maxkb4j.util.SpringUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class BaseChatNode extends IChatNode {

    private final ModelService modelService;

    public BaseChatNode() {
        this.modelService = SpringUtil.getBean(ModelService.class);
    }

    @Override
    public JSONObject getDetail(int index) {
        JSONObject detail = super.getDetail(index);
        detail.put("system", context.getString("system"));
        detail.put("history_message", new ArrayList<>());
        detail.put("question", context.getString("question"));
        detail.put("answer", context.getString("answer"));
        detail.put("message_tokens", context.getInteger("message_tokens"));
        detail.put("answer_tokens", context.getInteger("answer_tokens"));
        return detail;
    }


    private Iterator<String> writeContextStream(Map<String, Object> nodeVariable, Map<String, Object> workflowVariable, INode currentNode, WorkflowManage workflow) {
        long startTime = System.currentTimeMillis();
        ChatStream chatStream = (ChatStream) nodeVariable.get("result");
        chatStream.onCompleteCallback((response) -> {
            TokenUsage tokenUsage = response.tokenUsage();
            context.put("message_tokens", tokenUsage.inputTokenCount());
            context.put("answer_tokens", tokenUsage.outputTokenCount());
            context.put("answer", response.content().text());
            context.put("question", nodeVariable.get("question"));
            context.put("history_message", nodeVariable.get("history_message"));
            long runTime = System.currentTimeMillis() - context.getLongValue("start_time");
            System.out.println("耗时1 "+(System.currentTimeMillis()-startTime)+" ms");
            context.put("run_time", runTime/1000F);
        });
        return chatStream.getIterator();
    }

    @Override
    public NodeResult execute(ChatNodeParams nodeParams, FlowParams flowParams) {
        long startTime = System.currentTimeMillis();
        if (Objects.isNull(nodeParams.getDialogueType())) {
            nodeParams.setDialogueType("WORKFLOW");
        }
        if (Objects.isNull(nodeParams.getModelParamsSetting())) {
            nodeParams.setModelParamsSetting(getDefaultModelParamsSetting(nodeParams.getModelId()));
        }
        System.out.println("execute耗时1 "+(System.currentTimeMillis()-startTime)+" ms");
        BaseChatModel chatModel = modelService.getModelById(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        System.out.println("execute耗时2 "+(System.currentTimeMillis()-startTime)+" ms");
        List<ApplicationChatRecordEntity> historyMessage = getHistoryMessage(flowParams.getHistoryChatRecord(), nodeParams.getDialogueNumber(), nodeParams.getDialogueType(), super.runtimeNodeId);
        this.context.put("history_message", historyMessage);
        System.out.println("execute耗时3 "+(System.currentTimeMillis()-startTime)+" ms");
        UserMessage question = generatePromptQuestion(nodeParams.getPrompt());
        this.context.put("question", question.singleText());
        System.out.println("execute耗时4 "+(System.currentTimeMillis()-startTime)+" ms");
        String system = workflowManage.generatePrompt(nodeParams.getSystem());
        this.context.put("system", system);
        System.out.println("execute耗时5 "+(System.currentTimeMillis()-startTime)+" ms");
        List<ChatMessage> messageList = generateMessageList(system, question, historyMessage);
        this.context.put("message_list", messageList);
        System.out.println("execute耗时6 "+(System.currentTimeMillis()-startTime)+" ms");
        if (flowParams.getStream()) {
            ChatStream chatStream = chatModel.stream(messageList);
            System.out.println("execute耗时7 "+(System.currentTimeMillis()-startTime)+" ms");
            Map<String, Object> nodeVariable = Map.of(
                    "result", chatStream,
                    "chat_model", chatModel,
                    "message_list", messageList,
                    "history_message", historyMessage,
                    "question", question.singleText()
            );
            return new NodeResult(nodeVariable, Map.of(), this::writeContextStream);
        } else {
            Response<AiMessage> res = chatModel.generate(messageList);
            Map<String, Object> nodeVariable = Map.of(
                    "result", res,
                    "chat_model", chatModel,
                    "message_list", messageList,
                    "history_message", historyMessage,
                    "question", question.singleText()
            );
            return new NodeResult(nodeVariable, Map.of());
        }

    }

    private JSONObject getDefaultModelParamsSetting(String modelId) {
       // ModelEntity model = modelService.getCacheById(modelId);
        return new JSONObject();
    }


    public List<ChatMessage> generateMessageList(String system, UserMessage question, List<ApplicationChatRecordEntity> historyChatRecord) {
        List<ChatMessage> messageList = new ArrayList<>();
        if (StringUtils.isNotBlank(system)) {
            messageList.add(SystemMessage.from(system));
        }
        messageList.add(question);
        return messageList;
    }

    public List<ApplicationChatRecordEntity> getHistoryMessage(List<ApplicationChatRecordEntity> historyChatRecord, int dialogueNumber, String dialogueType, String runtimeNodeId) {
        int startIndex = Math.max(historyChatRecord.size() - dialogueNumber, 0);
        // 使用Stream API和flatMap来代替Python中的reduce操作
        return historyChatRecord.subList(startIndex, historyChatRecord.size());
    }

    public UserMessage generatePromptQuestion(String prompt) {
        return UserMessage.from(super.workflowManage.generatePrompt(prompt));
    }


    @Override
    public void saveContext(JSONObject detail, WorkflowManage workflowManage) {
        this.context.put("question", detail.get("question"));
        this.context.put("answer", detail.get("answer"));
        this.answerText = detail.getString("answer");
    }
}
