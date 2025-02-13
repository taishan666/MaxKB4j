package com.tarzan.maxkb4j.module.application.workflow.handler;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.chatpipeline.ChatCache;
import com.tarzan.maxkb4j.module.application.chatpipeline.ChatInfo;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationPublicAccessClientEntity;
import com.tarzan.maxkb4j.module.application.enums.AuthenticationType;
import com.tarzan.maxkb4j.module.application.service.ApplicationPublicAccessClientService;
import com.tarzan.maxkb4j.module.application.workflow.WorkflowManage;
import com.tarzan.maxkb4j.util.SpringUtil;
import lombok.Data;

import java.util.List;

@Data
public class WorkFlowPostHandler {

    private final ApplicationPublicAccessClientService publicAccessClientService;
    private ChatInfo chatInfo;
    private String clientId;
    private String clientType;

    public WorkFlowPostHandler(ChatInfo chatInfo, String clientId, String clientType) {
        this.publicAccessClientService = SpringUtil.getBean(ApplicationPublicAccessClientService.class);
        this.chatInfo = chatInfo;
        this.clientId = clientId;
        this.clientType = clientType;
    }

    public void handler(String chatId, String chatRecordId, String answer, WorkflowManage workflow) {
        String question = workflow.getParams().getQuestion();
        JSONObject details = workflow.getRuntimeDetails();

        int messageTokens = details.values().stream()
                .map(row -> (JSONObject) row)
                .filter(row -> row.containsKey("message_tokens") && row.get("message_tokens") != null)
                .mapToInt(row -> row.getIntValue("message_tokens"))
                .sum();

        int answerTokens = details.values().stream()
                .map(row -> (JSONObject) row)
                .filter(row -> row.containsKey("answer_tokens") && row.get("answer_tokens") != null)
                .mapToInt(row -> row.getIntValue("answer_tokens"))
                .sum();
        List<String> answerTextList = workflow.getAnswerTextList();
        ApplicationChatRecordEntity chatRecord;
        if (workflow.getChatRecord() != null) {
            chatRecord = workflow.getChatRecord();
            chatRecord.setAnswerText(answer);
            chatRecord.setDetails(new JSONObject(details));
            chatRecord.setMessageTokens(messageTokens);
            chatRecord.setAnswerTokens(answerTokens);
            chatRecord.setAnswerTextList(answerTextList);
            long startTime = workflow.getContext().getLong("start_time");
            chatRecord.setRunTime((System.currentTimeMillis() - startTime) / 1000F);
        } else {
            long startTime= workflow.getContext().getLongValue("start_time");
            chatRecord = new ApplicationChatRecordEntity();
            chatRecord.setId(chatRecordId);
            chatRecord.setChatId(chatId);
            chatRecord.setProblemText(question);
            chatRecord.setAnswerText(answer);
            chatRecord.setIndex(chatInfo.getChatRecordList().size() + 1);
            chatRecord.setMessageTokens(messageTokens);
            chatRecord.setAnswerTokens(answerTokens);
            chatRecord.setAnswerTextList(answerTextList);
            chatRecord.setRunTime((System.currentTimeMillis() - startTime) / 1000F);
            chatRecord.setVoteStatus("-1");
            chatRecord.setConstant(0);
            chatRecord.setDetails(details);
        }
        chatInfo.addChatRecord(chatRecord, clientId);
        // 重新设置缓存
        ChatCache.put(chatId, chatInfo);

        if (clientType!=null&&clientType.equals(AuthenticationType.APPLICATION_ACCESS_TOKEN.name())) {
            ApplicationPublicAccessClientEntity applicationPublicAccessClient = publicAccessClientService.getById(clientId);
            if (applicationPublicAccessClient != null) {
                applicationPublicAccessClient.setAccessNum(applicationPublicAccessClient.getAccessNum() + 1);
                applicationPublicAccessClient.setIntraDayAccessNum(applicationPublicAccessClient.getIntraDayAccessNum() + 1);
                publicAccessClientService.updateById(applicationPublicAccessClient);
            }
        }
    }

}

