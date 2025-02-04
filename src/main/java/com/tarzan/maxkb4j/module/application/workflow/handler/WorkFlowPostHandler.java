package com.tarzan.maxkb4j.module.application.workflow.handler;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.chatpipeline.ChatCache;
import com.tarzan.maxkb4j.module.application.chatpipeline.ChatInfo;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.workflow.WorkflowManage;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class WorkFlowPostHandler {

   // @Autowired
   // private ApplicationPublicAccessClientService accessClientService;

    private ChatInfo chatInfo;
    private String clientId;
    private String clientType;

    public WorkFlowPostHandler(ChatInfo chatInfo, String clientId, String clientType) {
        this.chatInfo = chatInfo;
        this.clientId = clientId;
        this.clientType = clientType;
    }

    public void handler(String chatId, String chatRecordId, String answer, WorkflowManage workflow) {
        String question = workflow.getParams().getQuestion();
        Map<String, JSONObject> details = workflow.getRuntimeDetails();

        int messageTokens = details.values().stream()
                .filter(row -> row.containsKey("message_tokens") && row.get("message_tokens") != null)
                .mapToInt(row -> ((Number)row.get("message_tokens")).intValue())
                .sum();

        int answerTokens = details.values().stream()
                .filter(row -> row.containsKey("answer_tokens") && row.get("answer_tokens") != null)
                .mapToInt(row -> ((Number)row.get("answer_tokens")).intValue())
                .sum();
        JSONObject finalDetails=new JSONObject();
        finalDetails.putAll(details);
        List<String> answerTextList = workflow.getAnswerTextList();
        StringBuilder answerText = new StringBuilder();
        for (String answer1: answerTextList) {
            answerText.append(answer1).append("\n\n");
        }

        ApplicationChatRecordEntity chatRecord;
        if (workflow.getChatRecord() != null) {
            chatRecord = workflow.getChatRecord();
            chatRecord.setAnswerText(answer);
            chatRecord.setDetails(finalDetails);
            chatRecord.setMessageTokens(messageTokens);
            chatRecord.setAnswerTokens(answerTokens);
            chatRecord.setAnswerTextList(answerTextList);
            long startTime = workflow.getContext().getLong("start_time");
            chatRecord.setRunTime((System.currentTimeMillis() - startTime) / 1000F);
        } else {
            long startTime= workflow.getContext().getLongValue("start_time");
            chatRecord = new ApplicationChatRecordEntity(chatRecordId, chatId, question, answerText.toString(), details,
                    messageTokens, answerTokens, answerTextList,
                    System.currentTimeMillis() - startTime, 0);
        }
        chatInfo.addChatRecord(chatRecord, clientId);
        // 重新设置缓存
        ChatCache.put(chatId, chatInfo);

       /* if (clientType.equals(AuthenticationType.APPLICATION_ACCESS_TOKEN.name())) {
            ApplicationPublicAccessClientEntity applicationPublicAccessClient = accessClientService.getById(clientId);
            if (applicationPublicAccessClient != null) {
                applicationPublicAccessClient.setAccessNum(applicationPublicAccessClient.getAccessNum() + 1);
                applicationPublicAccessClient.setIntraDayAccessNum(applicationPublicAccessClient.getIntraDayAccessNum() + 1);
                accessClientService.save(applicationPublicAccessClient);
            }
        }*/
    }

}

