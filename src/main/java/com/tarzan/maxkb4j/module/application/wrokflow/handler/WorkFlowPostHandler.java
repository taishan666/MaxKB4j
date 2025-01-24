package com.tarzan.maxkb4j.module.application.wrokflow.handler;

import com.tarzan.maxkb4j.module.application.chatpipeline.ChatInfo;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationPublicAccessClientEntity;
import com.tarzan.maxkb4j.module.application.enums.AuthenticationType;
import com.tarzan.maxkb4j.module.application.service.ApplicationPublicAccessClientService;
import com.tarzan.maxkb4j.module.application.wrokflow.WorkflowManage;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
@Data
public class WorkFlowPostHandler {

    @Autowired
    private ApplicationPublicAccessClientService ApplicationPublicAccessClientService;

    private ChatInfo chatInfo;
    private String clientId;
    private String clientType;

    public WorkFlowPostHandler(ChatInfo chatInfo, String clientId, String clientType) {
        this.chatInfo = chatInfo;
        this.clientId = clientId;
        this.clientType = clientType;
    }

    public void handler(String chatId, String chatRecordId, Map<String, Object> answer, WorkflowManage workflow) {
        String question = workflow.getParams().getQuestion();
        Map<String, Map<String, Object>> details = workflow.getRuntimeDetails();

        int messageTokens = details.values().stream()
                .filter(row -> row.containsKey("message_tokens") && row.get("message_tokens") != null)
                .mapToInt(row -> ((Number)row.get("message_tokens")).intValue())
                .sum();

        int answerTokens = details.values().stream()
                .filter(row -> row.containsKey("answer_tokens") && row.get("answer_tokens") != null)
                .mapToInt(row -> ((Number)row.get("answer_tokens")).intValue())
                .sum();

        List<Map<String, Object>> answerTextList = workflow.getAnswerTextList();
        StringBuilder answerText = new StringBuilder();
        for (Map<String, Object> answerEntry : answerTextList) {
            answerText.append(answerEntry.get("content")).append("\n\n");
        }

        ApplicationChatRecordEntity chatRecord;
        if (workflow.getChatRecord() != null) {
            chatRecord = workflow.getChatRecord();
            chatRecord.setAnswerText(answerText.toString());
            chatRecord.setDetails(details);
            chatRecord.setMessageTokens(messageTokens);
            chatRecord.setAnswerTokens(answerTokens);
            chatRecord.setAnswerTextList(answerTextList);
            chatRecord.setRunTime(Instant.now().getEpochSecond() - (Long)workflow.getContext().get("start_time"));
        } else {
            chatRecord = new ApplicationChatRecordEntity(chatRecordId, chatId, question, answerText.toString(), details,
                    messageTokens, answerTokens, answerTextList,
                    Instant.now().getEpochSecond() - (Long)workflow.getContext().get("start_time"), 0);
        }

        chatInfo.appendChatRecord(chatRecord, clientId);
        // 重新设置缓存
        ChatCache.set(chatId, chatInfo, 30 * 60);

        if (clientType.equals(AuthenticationType.APPLICATION_ACCESS_TOKEN.name())) {
            ApplicationPublicAccessClientEntity applicationPublicAccessClient = ApplicationPublicAccessClientService;
            if (applicationPublicAccessClient != null) {
                applicationPublicAccessClient.setAccessNum(applicationPublicAccessClient.getAccessNum() + 1);
                applicationPublicAccessClient.setIntradayAccessNum(applicationPublicAccessClient.getIntradayAccessNum() + 1);
                applicationPublicAccessClient.save();
            }
        }
    }
}

