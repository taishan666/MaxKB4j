package com.tarzan.maxkb4j.module.application.handler.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tarzan.maxkb4j.common.util.StringUtil;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatInfo;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatUserStatsEntity;
import com.tarzan.maxkb4j.module.application.enums.ChatUserType;
import com.tarzan.maxkb4j.module.application.handler.PostResponseHandler;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatMapper;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatRecordMapper;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatUserStatsService;
import com.tarzan.maxkb4j.module.chat.cache.ChatCache;
import com.tarzan.maxkb4j.module.chat.dto.ChatParams;
import com.tarzan.maxkb4j.module.chat.dto.ChatResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Component
public class ChatPostHandler implements PostResponseHandler {

    private final ApplicationChatUserStatsService chatUserStatsService;
    private final ApplicationChatMapper chatMapper;
    private final ApplicationChatRecordMapper chatRecordMapper;

    @Override
    public void handler(ChatParams chatParams, ChatResponse chatResponse, ApplicationChatRecordEntity chatRecord, long startTime) {
        String chatId = chatParams.getChatId();
        String chatRecordId = chatParams.getChatRecordId();
        String problemText = chatParams.getMessage();
        String chatUserId = chatParams.getChatUserId();
        String chatUserType = chatParams.getChatUserType();
        boolean debug = chatParams.getDebug();
        float runTime = (System.currentTimeMillis() - startTime) / 1000F;
        ChatInfo chatInfo = ChatCache.get(chatId);
        String answerText = chatResponse.getAnswer();
        int messageTokens = chatResponse.getMessageTokens();
        int answerTokens = chatResponse.getAnswerTokens();
        JSONObject details = chatResponse.getRunDetails();
        if (chatRecord != null) {
            chatRecord.setAnswerTextList(List.of(answerText));
            chatRecord.setAnswerText(answerText);
            chatRecord.setDetails(new JSONObject(details));
            chatRecord.setMessageTokens(messageTokens);
            chatRecord.setAnswerTokens(answerTokens);
            chatRecord.setCost(messageTokens + answerTokens);
            chatRecord.setRunTime(runTime + chatRecord.getRunTime());
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
            chatRecord.setRunTime(runTime);
            chatRecord.setVoteStatus("-1");
            chatRecord.setCost(messageTokens + answerTokens);
            chatRecord.setDetails(details);
            chatRecord.setImproveParagraphIdList(List.of());
        }
        chatInfo.addChatRecord(chatRecord);
        // 重新设置缓存
        ChatCache.put(chatId, chatInfo);
        if (!debug) {
            ApplicationChatUserStatsEntity chatUserStats = chatUserStatsService.getByUserIdAndAppId(chatUserId, chatInfo.getAppId());
            if (chatUserStats != null) {
                chatUserStats.setAccessNum(chatUserStats.getAccessNum() + 1);
                chatUserStats.setIntraDayAccessNum(chatUserStats.getIntraDayAccessNum() + 1);
                chatUserStatsService.updateById(chatUserStats);
            }
            long chatCount = chatMapper.selectCount(Wrappers.<ApplicationChatEntity>lambdaQuery().eq(ApplicationChatEntity::getId, chatId));
            if (chatCount == 0) {
                ApplicationChatEntity chatEntity = new ApplicationChatEntity();
                chatEntity.setId(chatId);
                chatEntity.setApplicationId(chatInfo.getAppId());
                String problemOverview = problemText.length() > 50 ? problemText.substring(0, 50) : problemText;
                chatEntity.setSummary(problemOverview);
                chatEntity.setChatUserId(chatUserId);
                chatEntity.setChatUserType(StringUtil.isBlank(chatUserType) ? ChatUserType.CHAT_USER.name() : chatUserType);
                chatEntity.setIsDeleted(false);
                chatEntity.setAsker(new JSONObject(Map.of("username", "游客")));
                chatEntity.setMeta(new JSONObject());
                chatEntity.setStarNum(0);
                chatEntity.setTrampleNum(0);
                chatEntity.setChatRecordCount(chatInfo.getChatRecordList().size());
                chatEntity.setMarkSum(0);
                chatMapper.insertOrUpdate(chatEntity);
            }
            chatRecordMapper.insertOrUpdate(chatRecord);
        }
    }


}

