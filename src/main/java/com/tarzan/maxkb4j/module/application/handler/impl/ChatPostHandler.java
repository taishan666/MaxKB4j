package com.tarzan.maxkb4j.module.application.handler.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tarzan.maxkb4j.module.application.cache.ChatCache;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatInfo;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatUserStatsEntity;
import com.tarzan.maxkb4j.module.application.enums.ChatUserType;
import com.tarzan.maxkb4j.module.application.handler.PostResponseHandler;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatMapper;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatRecordMapper;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatUserStatsService;
import com.tarzan.maxkb4j.util.StringUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Component
public class ChatPostHandler extends PostResponseHandler {

    private final ApplicationChatUserStatsService chatUserStatsService;
    private final ApplicationChatMapper chatMapper;
    private final ApplicationChatRecordMapper chatRecordMapper;


     //todo 优化
    @Override
    public void handler(String chatId, String chatRecordId, String problemText, String answerText,ApplicationChatRecordEntity chatRecord,   JSONObject details,long startTime, String chatUserId, String chatUserType,boolean debug) {
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
        float runTime = (System.currentTimeMillis() - startTime) / 1000F;
        if (chatRecord != null) {
            chatRecord.setAnswerTextList(List.of(chatRecord.getAnswerText(),answerText));
            chatRecord.setAnswerText(chatRecord.getAnswerText()+"\n\n"+answerText);
            chatRecord.setDetails(new JSONObject(details));
            chatRecord.setMessageTokens(messageTokens);
            chatRecord.setAnswerTokens(answerTokens);
            chatRecord.setCost(messageTokens+answerTokens);
            chatRecord.setRunTime(runTime+chatRecord.getRunTime());
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
            chatRecord.setCost(messageTokens+answerTokens);
            chatRecord.setDetails(details);
            chatRecord.setImproveParagraphIdList(List.of());
        }
        chatInfo.addChatRecord(chatRecord);
        // 重新设置缓存
        ChatCache.put(chatId, chatInfo);
       // chatMemoryStore.updateMessages(chatId, messages);
        if (!debug) {
            ApplicationChatUserStatsEntity applicationPublicAccessClient = chatUserStatsService.getById(chatUserId);
            if (applicationPublicAccessClient != null) {
                applicationPublicAccessClient.setAccessNum(applicationPublicAccessClient.getAccessNum() + 1);
                applicationPublicAccessClient.setIntraDayAccessNum(applicationPublicAccessClient.getIntraDayAccessNum() + 1);
                chatUserStatsService.updateById(applicationPublicAccessClient);
            }
            long chatRecordCount=chatRecordMapper.selectCount(Wrappers.<ApplicationChatRecordEntity>lambdaQuery().eq(ApplicationChatRecordEntity::getId, chatId));
            chatRecordMapper.insertOrUpdate(chatRecord);
            if(chatRecordCount==0){
                ApplicationChatEntity chatEntity = new ApplicationChatEntity();
                chatEntity.setId(chatId);
                chatEntity.setApplicationId(chatInfo.getAppId());
                String problemOverview=problemText.length()>50?problemText.substring(0,50):problemText;
                chatEntity.setSummary(problemOverview);
                chatEntity.setChatUserId(chatUserId);
                chatEntity.setChatUserType(StringUtil.isBlank(chatUserType)? ChatUserType.CHAT_USER.name() :chatUserType);
                chatEntity.setIsDeleted(false);
                chatEntity.setAsker(new JSONObject(Map.of("username","游客")));
                chatEntity.setMeta(new JSONObject());
                chatEntity.setStarNum(0);
                chatEntity.setTrampleNum(0);
                chatEntity.setChatRecordCount(chatInfo.getChatRecordList().size());
                chatEntity.setMarkSum(0);
                chatMapper.insertOrUpdate(chatEntity);
            }
        }
    }
}

