package com.tarzan.maxkb4j.module.application.workflow.node.aichatnode.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.workflow.ChatStream;
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
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import org.springframework.util.CollectionUtils;

import java.util.*;

public class BaseChatNode extends IChatNode {

    private final ModelService modelService;

    public BaseChatNode() {
        this.modelService = SpringUtil.getBean(ModelService.class);
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("system", context.get("system"));
        List<ChatMessage> historyMessage = (List<ChatMessage>) context.get("history_message");
        detail.put("history_message", resetMessageList(historyMessage));
        detail.put("question", context.get("question"));
        detail.put("answer", context.get("answer"));
        detail.put("message_tokens", context.get("message_tokens"));
        detail.put("answer_tokens", context.get("answer_tokens"));
        return detail;
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


    private Iterator<String> writeContextStream(Map<String, Object> nodeVariable, Map<String, Object> workflowVariable, INode currentNode, WorkflowManage workflow) {
        ChatStream chatStream = (ChatStream) nodeVariable.get("result");
        chatStream.onCompleteCallback((response) -> {
            String answer = response.aiMessage().text();
            workflow.setAnswer(answer);
            TokenUsage tokenUsage = response.tokenUsage();
            context.put("message_tokens", tokenUsage.inputTokenCount());
            context.put("answer_tokens", tokenUsage.outputTokenCount());
            context.put("answer", answer);
           // context.put("question", nodeVariable.get("question"));
          //  context.put("history_message", nodeVariable.get("history_message"));
            long runTime = System.currentTimeMillis() - (long)context.get("start_time");
            context.put("run_time", runTime / 1000F);
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
        System.out.println("execute耗时1 " + (System.currentTimeMillis() - startTime) + " ms");
        BaseChatModel chatModel = modelService.getModelById(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        System.out.println("execute耗时2 " + (System.currentTimeMillis() - startTime) + " ms");
        List<ChatMessage> historyMessage = getHistoryMessage(flowParams.getHistoryChatRecord(), nodeParams.getDialogueNumber(), nodeParams.getDialogueType(), super.runtimeNodeId);
        UserMessage question = generatePromptQuestion(nodeParams.getPrompt());
        String system = workflowManage.generatePrompt(nodeParams.getSystem());
        List<ChatMessage> messageList =  super.workflowManage.generateMessageList(system, question, historyMessage);
        if (flowParams.getStream()) {
            ChatStream chatStream = chatModel.stream(messageList);
            System.out.println("execute耗时7 " + (System.currentTimeMillis() - startTime) + " ms");
            Map<String, Object> nodeVariable = Map.of(
                    "result", chatStream,
                    "system", system,
                    "chat_model", chatModel,
                    "message_list", messageList,
                    "history_message", historyMessage,
                    "question", question.singleText()
            );
            return new NodeResult(nodeVariable, Map.of(), this::writeContextStream);
        } else {
            ChatResponse res = chatModel.generate(messageList);
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




    public List<ChatMessage> getHistoryMessage(List<ApplicationChatRecordEntity> historyChatRecord, int dialogueNumber, String dialogueType, String runtimeNodeId) {
        List<ChatMessage> historyMessage = new ArrayList<>();
        int startIndex = Math.max(historyChatRecord.size() - dialogueNumber, 0);
        // 遍历指定范围内的聊天记录
        for (int index = startIndex; index < historyChatRecord.size(); index++) {
            // 获取每条消息并添加到历史消息列表中
            historyMessage.addAll(getMessage(historyChatRecord.get(index), dialogueType, runtimeNodeId));
        }
        // 使用Stream API和flatMap来代替Python中的reduce操作
        return historyMessage;
    }

    private List<ChatMessage> getMessage(ApplicationChatRecordEntity chatRecord, String dialogueType, String runtimeNodeId) {
        if ("NODE".equals(dialogueType)) {
            return getNodeMessage(chatRecord, runtimeNodeId);
        } else {
            return getNodeMessage(chatRecord);
        }
    }

    public List<ChatMessage> getNodeMessage(ApplicationChatRecordEntity chatRecord) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new UserMessage(chatRecord.getProblemText()));
        messages.add(new AiMessage(chatRecord.getAnswerText()));
        return messages;
    }

    public List<ChatMessage> getNodeMessage(ApplicationChatRecordEntity chatRecord, String runtimeNodeId) {
        // 获取节点详情
        JSONObject nodeDetails = chatRecord.getNodeDetailsByRuntimeNodeId(runtimeNodeId);
        // 如果节点详情为空，返回空列表
        if (nodeDetails == null) {
            return new ArrayList<>();
        }
        // 创建消息列表
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new UserMessage(nodeDetails.getString("question")));
        messages.add(new AiMessage(nodeDetails.getString("answer")));

        return messages;
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
