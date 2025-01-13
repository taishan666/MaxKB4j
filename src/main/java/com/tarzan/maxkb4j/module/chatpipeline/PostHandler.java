package com.tarzan.maxkb4j.module.chatpipeline;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatRecordService;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import com.tarzan.maxkb4j.util.SpringUtil;
import lombok.AllArgsConstructor;

import java.util.*;

@AllArgsConstructor
public class PostHandler extends PostResponseHandler{

    private ChatInfo chatInfo;

    @Override
    public void handler(UUID chatId, UUID chatRecordId, List<ParagraphVO> paragraphList, String problemText, String answerText, PipelineManage manage, String paddingProblemText, UUID clientId) {
        System.out.println("PostHandler");
        JSONObject context = manage.getContext();
        ApplicationChatRecordEntity  chatRecord=new ApplicationChatRecordEntity();
        chatRecord.setId(chatRecordId);
        chatRecord.setChatId(chatId);
        chatRecord.setProblemText(problemText);
        chatRecord.setAnswerText(answerText);
        chatRecord.setIndex(chatInfo.getChatRecordList().size()+1);
        chatRecord.setMessageTokens(context.getInteger("message_tokens"));
        chatRecord.setAnswerTokens(context.getInteger("answer_tokens"));
        chatRecord.setAnswerTextList(Set.of(answerText));
        chatRecord.setRunTime(context.getDouble("run_time"));
        chatRecord.setVoteStatus("-1");
        chatRecord.setConstant(0);
        chatRecord.setDetails(manage.getDetails());
        chatRecord.setImproveParagraphIdList(new UUID[0]);
        ApplicationChatRecordService chatRecordService= SpringUtil.getBean(ApplicationChatRecordService.class);
        chatRecordService.save(chatRecord);
        chatInfo.getChatRecordList().add(chatRecord);
        ChatCache.put(chatInfo.getChatId(),chatInfo);
    }

}
