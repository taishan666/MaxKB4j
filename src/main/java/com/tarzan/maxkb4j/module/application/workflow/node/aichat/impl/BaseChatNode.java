package com.tarzan.maxkb4j.module.application.workflow.node.aichat.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.workflow.ChatStream;
import com.tarzan.maxkb4j.module.application.workflow.INode;
import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.WorkflowManage;
import com.tarzan.maxkb4j.module.application.workflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.workflow.node.aichat.IChatNode;
import com.tarzan.maxkb4j.module.application.workflow.node.aichat.input.ChatNodeParams;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.util.SpringUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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


    private Iterator<String> writeContextStream(Map<String, Object> nodeVariable, Map<String, Object> workflowVariable, INode currentNode, WorkflowManage workflow) {
        ChatStream chatStream = (ChatStream) nodeVariable.get("result");
        chatStream.onCompleteCallback((response) -> {
            String answer = response.aiMessage().text();
            workflow.setAnswer(answer);
            TokenUsage tokenUsage = response.tokenUsage();
            context.put("message_tokens", tokenUsage.inputTokenCount());
            context.put("answer_tokens", tokenUsage.outputTokenCount());
            context.put("answer", answer);
            context.put("question", nodeVariable.get("question"));
            context.put("history_message", nodeVariable.get("history_message"));
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
        List<ChatMessage> historyMessage = workflowManage.getHistoryMessage(flowParams.getHistoryChatRecord(), nodeParams.getDialogueNumber(), nodeParams.getDialogueType(), super.runtimeNodeId);
        UserMessage question =  workflowManage.generatePromptQuestion(nodeParams.getPrompt());
        String system = workflowManage.generatePrompt(nodeParams.getSystem());
        List<ChatMessage> messageList =  super.workflowManage.generateMessageList(system, question, historyMessage);
        if (nodeParams.getIsResult()) {
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
            AiMessage aiMessage = res.aiMessage();
            TokenUsage tokenUsage = res.tokenUsage();
            Map<String, Object> nodeVariable = Map.of(
                    "system", system,
                    "chat_model", chatModel,
                    "message_list", messageList,
                    "history_message", historyMessage,
                    "question", question.singleText(),
                    "answer", aiMessage.text(),
                    "message_tokens", tokenUsage.inputTokenCount(),
                    "answer_tokens", tokenUsage.outputTokenCount()
            );
            return new NodeResult(nodeVariable, Map.of());
        }

    }

    private JSONObject getDefaultModelParamsSetting(String modelId) {
        // ModelEntity model = modelService.getCacheById(modelId);
        return new JSONObject();
    }





    @Override
    public void saveContext(JSONObject detail, WorkflowManage workflowManage) {
        this.context.put("question", detail.get("question"));
        this.context.put("answer", detail.get("answer"));
        this.answerText = detail.getString("answer");
    }
}
