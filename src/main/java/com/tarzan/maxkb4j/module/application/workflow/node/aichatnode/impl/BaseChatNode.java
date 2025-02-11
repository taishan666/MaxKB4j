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
import com.tarzan.maxkb4j.module.model.entity.ModelEntity;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import com.tarzan.maxkb4j.module.model.service.ModelService;
import com.tarzan.maxkb4j.util.SpringUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BaseChatNode extends IChatNode {

    private final ModelService modelService;

    public BaseChatNode() {
        this.modelService = SpringUtil.getBean(ModelService.class);
    }

    @Override
    public JSONObject getDetail(int index) {
        JSONObject detail = new JSONObject();
        detail.put("name", node.getProperties().getString("name"));
        detail.put("index", index);
        detail.put("run_time", context.getFloatValue("run_time"));
        detail.put("system", context.getString("system"));
        detail.put("history_message", new ArrayList<>());
        detail.put("question", context.getString("question"));
        detail.put("answer", context.getString("answer"));
        detail.put("type", node.getType());
        detail.put("message_tokens", context.getInteger("message_tokens"));
        detail.put("answer_tokens", context.getInteger("answer_tokens"));
        detail.put("status", context.getInteger("status"));
        detail.put("err_message", context.getString("err_message"));
        return detail;
    }


    private ChatStream writeContextStream(Map<String, Object> nodeVariable, Map<String, Object> workflowVariable, INode currentNode, WorkflowManage workflow){
        return (ChatStream) nodeVariable.get("result");
    }


    @Override
    public NodeResult execute(ChatNodeParams nodeParams, FlowParams flowParams) {
        if (Objects.isNull(nodeParams.getDialogueType())) {
            nodeParams.setDialogueType("WORKFLOW");
        }
        if (Objects.isNull(nodeParams.getModelParamsSetting())) {
            nodeParams.setModelParamsSetting(getDefaultModelParamsSetting(nodeParams.getModelId()));
        }
        BaseChatModel chatModel = modelService.getModelById(nodeParams.getModelId(),nodeParams.getModelParamsSetting());
        List<ApplicationChatRecordEntity> historyMessage = getHistoryMessage(flowParams.getHistoryChatRecord(), nodeParams.getDialogueNumber(), nodeParams.getDialogueType(), super.runtimeNodeId);
        this.context.put("history_message", historyMessage);
        UserMessage question = generatePromptQuestion(nodeParams.getPrompt());
        this.context.put("question", question.singleText());
        List<ChatMessage> messageList = generateMessageList(nodeParams.getSystem(), question , historyMessage);
        this.context.put("message_list", messageList);
        if(flowParams.getStream()){
            ChatStream chatStream=chatModel.stream(messageList);
            Map<String, Object> nodeVariable = Map.of(
                    "result", chatStream,
                    "chat_model", chatModel,
                    "answer", "测试",
                    "message_list", messageList,
                    "history_message", historyMessage,
                    "question", question.singleText()
            );
            return new NodeResult(nodeVariable, Map.of(),this::writeContextStream);
        }else {
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
        ModelEntity model = modelService.getById(modelId);
        return new JSONObject();
    }


    public List<ChatMessage> generateMessageList(String system, UserMessage question , List<ApplicationChatRecordEntity> historyChatRecord) {
        List<ChatMessage> messageList = new ArrayList<>();
        if(StringUtils.isNotBlank(system)){
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
    public void saveContext(JSONObject nodeDetail, WorkflowManage workflowManage) {
        this.context.put("question", nodeDetail.get("question"));
        this.context.put("answer", nodeDetail.get("answer"));
        this.answerText=nodeDetail.getString("answer");
    }
}
