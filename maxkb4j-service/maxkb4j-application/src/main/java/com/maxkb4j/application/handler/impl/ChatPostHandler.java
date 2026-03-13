package com.maxkb4j.application.handler.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maxkb4j.common.cache.ChatCache;
import com.maxkb4j.common.domain.dto.ChatInfo;
import com.maxkb4j.application.entity.ApplicationChatEntity;
import com.maxkb4j.application.entity.ApplicationChatRecordEntity;
import com.maxkb4j.application.entity.ApplicationChatUserStatsEntity;
import com.maxkb4j.application.handler.PostResponseHandler;
import com.maxkb4j.application.mapper.ApplicationChatMapper;
import com.maxkb4j.application.mapper.ApplicationChatRecordMapper;
import com.maxkb4j.application.service.ApplicationChatUserStatsService;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatRecordDTO;
import com.maxkb4j.common.domain.dto.ChatResponse;
import com.maxkb4j.common.enums.ChatUserType;
import com.maxkb4j.common.util.BeanUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class ChatPostHandler implements PostResponseHandler {

    private final ApplicationChatUserStatsService chatUserStatsService;
    private final ApplicationChatMapper chatMapper;
    private final ApplicationChatRecordMapper chatRecordMapper;

    @Override
    public void handler(ChatParams chatParams, ChatResponse chatResponse, long startTime) {
        String chatId = chatParams.getChatId();
        String chatRecordId = chatParams.getChatRecordId();
        String problemText = chatParams.getMessage();
        String chatUserId = chatParams.getChatUserId();
        String chatUserType = chatParams.getChatUserType();
        boolean debug = chatParams.getDebug();
        float runTime = (System.currentTimeMillis() - startTime) / 1000F;
        ChatInfo chatInfo = ChatCache.get(chatId);
        String answerText = chatResponse.getAnswer();
        JSONArray answerTextList=chatResponse.getAnswerJSONArray();
        int messageTokens = chatResponse.getMessageTokens();
        int answerTokens = chatResponse.getAnswerTokens();
        JSONObject details = chatResponse.getRunDetails();
        ChatRecordDTO chatRecord=chatParams.getChatRecord();
        ApplicationChatRecordEntity chatRecordEntity = new ApplicationChatRecordEntity();
        if (chatRecord != null) {
            chatRecordEntity.setAnswerText(answerText);
            chatRecordEntity.setAnswerTextList(answerTextList);
            chatRecordEntity.setDetails(new JSONObject(details));
            chatRecordEntity.setMessageTokens(messageTokens);
            chatRecordEntity.setAnswerTokens(answerTokens);
            chatRecordEntity.setCost(messageTokens + answerTokens);
            chatRecordEntity.setRunTime(runTime + chatRecord.getRunTime());
        } else {
            chatRecordEntity.setId(chatRecordId);
            chatRecordEntity.setChatId(chatId);
            chatRecordEntity.setProblemText(problemText);
            chatRecordEntity.setAnswerText(answerText);
            chatRecordEntity.setAnswerTextList(answerTextList);
            if (chatInfo!=null){
                chatRecordEntity.setIndex(chatInfo.getChatRecordList().size() + 1);
            }else {
                chatRecordEntity.setIndex(0);
            }
            chatRecordEntity.setMessageTokens(messageTokens);
            chatRecordEntity.setAnswerTokens(answerTokens);
            chatRecordEntity.setRunTime(runTime);
            chatRecordEntity.setVoteStatus("-1");
            chatRecordEntity.setCost(messageTokens + answerTokens);
            chatRecordEntity.setDetails(details);
            chatRecordEntity.setImproveParagraphIdList(List.of());
        }
        assert chatInfo != null;
        chatInfo.addChatRecord(BeanUtil.copy(chatRecord, ChatRecordDTO.class));
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
            ApplicationChatEntity chatEntity = new ApplicationChatEntity();
            chatEntity.setId(chatId);
            if (chatCount == 0) {
                chatEntity.setApplicationId(chatInfo.getAppId());
                String problemOverview = problemText.length() > 50 ? problemText.substring(0, 50) : problemText;
                chatEntity.setSummary(problemOverview);
                chatEntity.setChatUserId(chatUserId);
                chatEntity.setChatUserType(StringUtils.isBlank(chatUserType) ? ChatUserType.CHAT_USER.name() : chatUserType);
                chatEntity.setIsDeleted(false);
                chatEntity.setAsker(new JSONObject(Map.of("username", "游客")));
                chatEntity.setMeta(new JSONObject());
                chatEntity.setStarNum(0);
                chatEntity.setTrampleNum(0);
                chatEntity.setChatRecordCount(1);
                chatEntity.setMarkSum(0);
                chatMapper.insert(chatEntity);
            }else {
                chatEntity.setChatRecordCount(chatInfo.getChatRecordList().size());
                chatMapper.updateById(chatEntity);
            }
            chatRecordMapper.insertOrUpdate(chatRecordEntity);
        }
    }


}

