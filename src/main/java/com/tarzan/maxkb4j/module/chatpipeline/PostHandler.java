package com.tarzan.maxkb4j.module.chatpipeline;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
public class PostHandler extends PostResponseHandler{

    private ChatInfo chatInfo;

    @Override
    public void handler(UUID chatId,UUID chatRecordId, String problemText,String answerText,PipelineManage manage,UUID clientId) {
        JSONObject context = manage.getContext();
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
        chatInfo.getChatRecordList().add(chatRecord);
        ChatCache.put(chatInfo.getChatId(),chatInfo);
    }

}
