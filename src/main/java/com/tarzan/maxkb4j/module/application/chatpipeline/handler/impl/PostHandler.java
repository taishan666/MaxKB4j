package com.tarzan.maxkb4j.module.application.chatpipeline.handler.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.cache.ChatCache;
import com.tarzan.maxkb4j.module.application.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.application.chatpipeline.handler.PostResponseHandler;
import com.tarzan.maxkb4j.module.application.dto.ChatInfo;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatMapper;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatRecordMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@AllArgsConstructor
@Component
public class PostHandler extends PostResponseHandler {

    private final ApplicationChatMapper chatMapper;
    private final ApplicationChatRecordMapper chatRecordMapper;

    @Override
    public void handler(ChatInfo chatInfo, String chatId, String chatRecordId, String problemText, String answerText, PipelineManage manage, String clientId) {
        long startTime = manage.context.getLong("start_time");
        JSONObject context = manage.context;
        ApplicationChatRecordEntity chatRecord = new ApplicationChatRecordEntity();
        chatRecord.setId(chatRecordId);
        chatRecord.setChatId(chatId);
        chatRecord.setProblemText(problemText);
        chatRecord.setAnswerText(answerText);
        chatRecord.setIndex(chatInfo.getChatRecordList().size() + 1);
        chatRecord.setMessageTokens(context.getInteger("messageTokens"));
        chatRecord.setAnswerTokens(context.getInteger("answerTokens"));
        chatRecord.setAnswerTextList(List.of(answerText));
        chatRecord.setRunTime((System.currentTimeMillis() - startTime) / 1000F);
        chatRecord.setVoteStatus("-1");
        chatRecord.setConstant(0);
        chatRecord.setDetails(manage.getDetails());
        chatRecord.setImproveParagraphIdList(new String[0]);
        chatInfo.getChatRecordList().add(chatRecord);
        ChatCache.put(chatInfo.getChatId(), chatInfo);
        if (Objects.nonNull(chatInfo.getApplication().getId())) {
            ApplicationChatEntity chatEntity = new ApplicationChatEntity();
            chatEntity.setId(chatId);
            String appId = chatInfo.getApplication().getId();
            chatEntity.setApplicationId(appId);
            problemText=problemText.length()>50?problemText.substring(0,50):problemText;
            chatEntity.setDigest(problemText);
            chatEntity.setClientId(clientId);
            chatEntity.setIsDeleted(false);
            chatMapper.insertOrUpdate(chatEntity);
            chatRecordMapper.insert(chatRecord);
        }
    }

}
