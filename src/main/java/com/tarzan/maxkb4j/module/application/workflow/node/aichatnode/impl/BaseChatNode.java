package com.tarzan.maxkb4j.module.application.workflow.node.aichatnode.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.workflow.INode;
import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.WorkflowManage;
import com.tarzan.maxkb4j.module.application.workflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.workflow.node.NodeDetail;
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

import java.util.*;

public class BaseChatNode extends IChatNode {

    private final ModelService modelService;

    public BaseChatNode() {
        this.modelService = SpringUtil.getBean(ModelService.class);
    }


    private Iterator<AiMessage> writeContextStream(Map<String, Object> nodeVariable, Map<String, Object> workflowVariable, INode currentNode, WorkflowManage workflow){
        return (Iterator<AiMessage>) nodeVariable.get("result");
        //TODO
      //  return writeContext(nodeVariable,workflowVariable,currentNode,workflow);
    }
    private static JSONObject  writeContext(Map<String, Object> nodeVariable, Map<String, Object> workflowVariable, INode currentNode, WorkflowManage workflow){
        Object response= nodeVariable.get("result");
        //TODO
        return null;
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
            Iterator<AiMessage> res=chatModel.stream(messageList);
            Map<String, Object> nodeVariable = Map.of(
                    "result", res,
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
    public void saveContext(NodeDetail nodeDetail, WorkflowManage workflowManage) {
        this.context.put("question", nodeDetail.getQuestion());
        this.context.put("answer", nodeDetail.getAnswer());
        this.answerText=nodeDetail.getAnswer();
    }
}
