package com.tarzan.maxkb4j.module.application.handler.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.cache.ChatCache;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatInfo;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatUserStatsEntity;
import com.tarzan.maxkb4j.module.application.enums.AuthType;
import com.tarzan.maxkb4j.module.application.handler.PostResponseHandler;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatMapper;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatRecordMapper;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatUserStatsService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@AllArgsConstructor
@Component
public class ChatPostHandler extends PostResponseHandler {

    private final ApplicationChatUserStatsService chatUserStatsService;
    private final ApplicationChatMapper chatMapper;
    private final ApplicationChatRecordMapper chatRecordMapper;


    @Override
    public void handler(String chatId, String chatRecordId, String problemText, String answerText, ApplicationChatRecordEntity chatRecord, JSONObject details,long startTime, String chatUserId, String chatUserType) {
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
        if (chatRecord != null) {
            chatRecord.setAnswerText(answerText);
            chatRecord.setDetails(new JSONObject(details));
            chatRecord.setMessageTokens(messageTokens);
            chatRecord.setAnswerTokens(answerTokens);
            chatRecord.setCost(messageTokens+answerTokens);
            chatRecord.setAnswerTextList(List.of(answerText));
            chatRecord.setRunTime((System.currentTimeMillis() - startTime) / 1000F);
        } else {
            chatRecord = new ApplicationChatRecordEntity();
            chatRecord.setId(chatRecordId);
            chatRecord.setChatId(chatId);
            chatRecord.setProblemText(problemText);
            chatRecord.setAnswerText(answerText);
            chatRecord.setAnswerTextList(List.of(answerText));
            chatRecord.setIndex(chatInfo.getChatRecordList().size() + 1);
            chatRecord.setMessageTokens(messageTokens);
            chatRecord.setAnswerTokens(answerTokens);
            chatRecord.setRunTime((System.currentTimeMillis() - startTime) / 1000F);
            chatRecord.setVoteStatus("-1");
            chatRecord.setCost(messageTokens+answerTokens);
            chatRecord.setDetails(details);
            chatRecord.setImproveParagraphIdList(List.of());
        }
        chatInfo.addChatRecord(chatRecord);
        // 重新设置缓存
        ChatCache.put(chatId, chatInfo);
       // chatMemoryStore.updateMessages(chatId, messages);
        if (AuthType.ACCESS_TOKEN.name().equals(chatUserType)) {
            ApplicationChatUserStatsEntity applicationPublicAccessClient = chatUserStatsService.getById(chatUserId);
            if (applicationPublicAccessClient != null) {
                applicationPublicAccessClient.setAccessNum(applicationPublicAccessClient.getAccessNum() + 1);
                applicationPublicAccessClient.setIntraDayAccessNum(applicationPublicAccessClient.getIntraDayAccessNum() + 1);
                chatUserStatsService.updateById(applicationPublicAccessClient);
            }
            String appId = chatInfo.getApplication().getId();
            if (Objects.nonNull(appId)) {
                if(chatInfo.getChatRecordList().size()==1){
                    ApplicationChatEntity chatEntity = new ApplicationChatEntity();
                    chatEntity.setId(chatId);
                    chatEntity.setApplicationId(appId);
                    String problemOverview=problemText.length()>50?problemText.substring(0,50):problemText;
                    chatEntity.setSummary(problemOverview);
                    chatEntity.setChatUserId(chatUserId);
                    chatEntity.setChatUserType(chatUserType);
                    chatEntity.setIsDeleted(false);
                    chatMapper.insertOrUpdate(chatEntity);
                }
                chatRecordMapper.insertOrUpdate(chatRecord);
            }
        }
    }
}

