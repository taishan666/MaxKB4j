/*
package com.tarzan.maxkb4j.module.application.handler.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.cache.ChatCache;
import com.tarzan.maxkb4j.module.application.dto.ChatInfo;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationPublicAccessClientEntity;
import com.tarzan.maxkb4j.module.application.enums.AuthType;
import com.tarzan.maxkb4j.module.application.handler.PostResponseHandler;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatMapper;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatRecordMapper;
import com.tarzan.maxkb4j.module.application.service.ApplicationPublicAccessClientService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

@AllArgsConstructor
@Component
public class ChatSimplePostHandler extends PostResponseHandler {

    private final ApplicationChatMapper chatMapper;
    private final ApplicationChatRecordMapper chatRecordMapper;
    private final ApplicationPublicAccessClientService publicAccessClientService;

    @Override
    public void handler(String chatId, String chatRecordId, String problemText, String answerText, ApplicationChatRecordEntity chatRecord, JSONObject details, long startTime, String clientId, String clientType) {
        ChatInfo chatInfo = ChatCache.get(chatId);
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
        chatRecord = new ApplicationChatRecordEntity();
        chatRecord.setId(chatRecordId);
        chatRecord.setChatId(chatId);
        chatRecord.setProblemText(problemText);
        chatRecord.setAnswerText(answerText);
        chatRecord.setIndex(chatInfo.getChatRecordList().size() + 1);
        chatRecord.setMessageTokens(messageTokens);
        chatRecord.setAnswerTokens(answerTokens);
        chatRecord.setAnswerTextList(Set.of(answerText));
        chatRecord.setRunTime((System.currentTimeMillis() - startTime) / 1000F);
        chatRecord.setVoteStatus("-1");
        chatRecord.setCost(0);
        chatRecord.setDetails(details);
        chatRecord.setImproveParagraphIdList(Set.of());
        chatInfo.getChatRecordList().add(chatRecord);
        ChatCache.put(chatInfo.getChatId(), chatInfo);
        if (Objects.nonNull(chatInfo.getApplication().getId())) {
            ApplicationChatEntity chatEntity = new ApplicationChatEntity();
            chatEntity.setId(chatId);
            String appId = chatInfo.getApplication().getId();
            chatEntity.setApplicationId(appId);
            problemText = problemText.length() > 50 ? problemText.substring(0, 50) : problemText;
            chatEntity.setOverview(problemText);
            chatEntity.setClientId(clientId);
            chatEntity.setIsDeleted(false);
            chatMapper.insertOrUpdate(chatEntity);
            chatRecordMapper.insert(chatRecord);
        }
        if (clientType!=null&&clientType.equals(AuthType.ACCESS_TOKEN.name())) {
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
                    String problemOverview=problemText.length()>50?problemText.substring(0,50):problemText;
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
*/
