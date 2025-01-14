package com.tarzan.maxkb4j.module.chatpipeline.handler.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatRecordService;
import com.tarzan.maxkb4j.module.chatpipeline.ChatCache;
import com.tarzan.maxkb4j.module.chatpipeline.ChatInfo;
import com.tarzan.maxkb4j.module.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.chatpipeline.handler.PostResponseHandler;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class PostHandler extends PostResponseHandler {

    @Autowired
    private ApplicationChatRecordService chatRecordService;

    @Override
    public void handler(ChatInfo chatInfo,UUID chatId, UUID chatRecordId, List<ParagraphVO> paragraphList, String problemText, String answerText, PipelineManage manage, String paddingProblemText, UUID clientId) {
        JSONObject context = manage.context;
        ApplicationChatRecordEntity  chatRecord=new ApplicationChatRecordEntity();
        chatRecord.setId(chatRecordId);
        chatRecord.setChatId(chatId);
        chatRecord.setProblemText(problemText);
        chatRecord.setAnswerText(answerText);
        chatRecord.setIndex(chatInfo.getChatRecordList().size()+1);
        chatRecord.setMessageTokens(context.getInteger("message_tokens"));
        chatRecord.setAnswerTokens(context.getInteger("answer_tokens"));
        chatRecord.setAnswerTextList(List.of(answerText));
        chatRecord.setRunTime(context.getDouble("run_time"));
        chatRecord.setVoteStatus("-1");
        chatRecord.setConstant(0);
        chatRecord.setDetails(manage.getDetails());
        chatRecord.setImproveParagraphIdList(new UUID[0]);
        chatInfo.getChatRecordList().add(chatRecord);
        ChatCache.put(chatInfo.getChatId(),chatInfo);
        chatRecordService.save(chatRecord);
    }

}
