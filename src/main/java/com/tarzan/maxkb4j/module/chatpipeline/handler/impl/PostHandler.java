package com.tarzan.maxkb4j.module.chatpipeline.handler.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatRecordService;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatService;
import com.tarzan.maxkb4j.module.chatpipeline.ChatCache;
import com.tarzan.maxkb4j.module.chatpipeline.ChatInfo;
import com.tarzan.maxkb4j.module.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.chatpipeline.handler.PostResponseHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class PostHandler extends PostResponseHandler {

    @Autowired
    private ApplicationChatService chatService;
    @Autowired
    private ApplicationChatRecordService chatRecordService;

    @Override
    public void handler(ChatInfo chatInfo, UUID chatId, UUID chatRecordId, String problemText, String answerText, PipelineManage manage, UUID clientId) {
        long startTime = manage.context.getLong("start_time");
        JSONObject context = manage.context;
        ApplicationChatRecordEntity chatRecord = new ApplicationChatRecordEntity();
        chatRecord.setId(chatRecordId);
        chatRecord.setChatId(chatId);
        chatRecord.setProblemText(problemText);
        chatRecord.setAnswerText(answerText);
        chatRecord.setIndex(chatInfo.getChatRecordList().size() + 1);
        chatRecord.setMessageTokens(context.getInteger("message_tokens"));
        chatRecord.setAnswerTokens(context.getInteger("answer_tokens"));
        chatRecord.setAnswerTextList(List.of(answerText));
        chatRecord.setRunTime((System.currentTimeMillis() - startTime) / 1000F);
        chatRecord.setVoteStatus("-1");
        chatRecord.setConstant(0);
        chatRecord.setDetails(manage.getDetails());
        chatRecord.setImproveParagraphIdList(new UUID[0]);
        chatInfo.getChatRecordList().add(chatRecord);
        ChatCache.put(chatInfo.getChatId(), chatInfo);
        if (Objects.nonNull(chatInfo.getApplication().getId())) {
            ApplicationChatEntity chatEntity = new ApplicationChatEntity();
            chatEntity.setId(chatId);
            UUID appId = chatInfo.getApplication().getId();
            chatEntity.setApplicationId(appId);
            problemText=problemText.length()>50?problemText.substring(0,50):problemText;
            chatEntity.setDigest(problemText);
            chatEntity.setClientId(clientId);
            chatEntity.setIsDeleted(false);
            chatService.saveOrUpdate(chatEntity);
            chatRecordService.save(chatRecord);
        }
    }

}
