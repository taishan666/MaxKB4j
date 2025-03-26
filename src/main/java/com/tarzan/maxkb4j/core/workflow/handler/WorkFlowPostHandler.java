package com.tarzan.maxkb4j.core.workflow.handler;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.WorkflowManage;
import com.tarzan.maxkb4j.module.application.cache.ChatCache;
import com.tarzan.maxkb4j.module.application.dto.ChatInfo;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationPublicAccessClientEntity;
import com.tarzan.maxkb4j.module.application.enums.AuthType;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatMapper;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatRecordMapper;
import com.tarzan.maxkb4j.module.application.service.ApplicationPublicAccessClientService;
import com.tarzan.maxkb4j.util.SpringUtil;
import lombok.Data;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Data
public class WorkFlowPostHandler {

    private final ApplicationPublicAccessClientService publicAccessClientService;
    private final ApplicationChatMapper chatMapper;
    private final ApplicationChatRecordMapper chatRecordMapper;
    private ChatInfo chatInfo;
    private String clientId;
    private String clientType;

    public WorkFlowPostHandler(ChatInfo chatInfo, String clientId, String clientType) {
        this.publicAccessClientService = SpringUtil.getBean(ApplicationPublicAccessClientService.class);
        this.chatMapper = SpringUtil.getBean(ApplicationChatMapper.class);
        this.chatRecordMapper = SpringUtil.getBean(ApplicationChatRecordMapper.class);
        this.chatInfo = chatInfo;
        this.clientId = clientId;
        this.clientType = clientType;
    }

    @Transactional
    public void handler(String chatId, String chatRecordId, String answer, WorkflowManage workflow) {
        String question = workflow.getParams().getQuestion();
        JSONObject details = workflow.getRuntimeDetails();
        int messageTokens = details.values().stream()
                .map(row -> (JSONObject) row)
                .filter(row -> row.containsKey("messageTokens") && row.get("messageTokens") != null)
                .mapToInt(row -> row.getIntValue("messageTokens"))
                .sum();

        int answerTokens = details.values().stream()
                .map(row -> (JSONObject) row)
                .filter(row -> row.containsKey("answerTokens") && row.get("answerTokens") != null)
                .mapToInt(row -> row.getIntValue("answerTokens"))
                .sum();
        ApplicationChatRecordEntity chatRecord;
        if (workflow.getChatRecord() != null) {
            chatRecord = workflow.getChatRecord();
            chatRecord.setAnswerText(answer);
            chatRecord.setDetails(new JSONObject(details));
            chatRecord.setMessageTokens(messageTokens);
            chatRecord.setAnswerTokens(answerTokens);
            chatRecord.setCost(messageTokens+answerTokens);
            chatRecord.setAnswerTextList(List.of(answer));
            long startTime = workflow.getContext().getLong("start_time");
            chatRecord.setRunTime((System.currentTimeMillis() - startTime) / 1000F);
        } else {
            long startTime= workflow.getContext().getLongValue("start_time");
            chatRecord = new ApplicationChatRecordEntity();
            chatRecord.setId(chatRecordId);
            chatRecord.setChatId(chatId);
            chatRecord.setProblemText(question);
            chatRecord.setAnswerText(answer);
            chatRecord.setAnswerTextList(List.of(answer));
            chatRecord.setIndex(chatInfo.getChatRecordList().size() + 1);
            chatRecord.setMessageTokens(messageTokens);
            chatRecord.setAnswerTokens(answerTokens);
            chatRecord.setRunTime((System.currentTimeMillis() - startTime) / 1000F);
            chatRecord.setVoteStatus("-1");
            chatRecord.setCost(messageTokens+answerTokens);
            chatRecord.setDetails(details);
            chatRecord.setImproveParagraphIdList(new String[0]);
        }
        chatInfo.addChatRecord(chatRecord);
        // 重新设置缓存
        ChatCache.put(chatId, chatInfo);

        if (clientType!=null&&clientType.equals(AuthType.APP_ACCESS_TOKEN.name())) {
            ApplicationPublicAccessClientEntity applicationPublicAccessClient = publicAccessClientService.getById(clientId);
            if (applicationPublicAccessClient != null) {
                applicationPublicAccessClient.setAccessNum(applicationPublicAccessClient.getAccessNum() + 1);
                applicationPublicAccessClient.setIntraDayAccessNum(applicationPublicAccessClient.getIntraDayAccessNum() + 1);
                publicAccessClientService.updateById(applicationPublicAccessClient);
            }
            String appId = chatInfo.getApplication().getId();
            if (Objects.nonNull(appId)) {
                if(chatInfo.getChatRecordList().size()==1){
                    ApplicationChatEntity chatEntity = new ApplicationChatEntity();
                    chatEntity.setId(chatId);
                    chatEntity.setApplicationId(appId);
                    String problemOverview=question.length()>50?question.substring(0,50):question;
                    chatEntity.setOverview(problemOverview);
                    chatEntity.setClientId(clientId);
                    chatEntity.setIsDeleted(false);
                    chatMapper.insertOrUpdate(chatEntity);
                }
                chatRecordMapper.insertOrUpdate(chatRecord);
            }
        }
    }

}

