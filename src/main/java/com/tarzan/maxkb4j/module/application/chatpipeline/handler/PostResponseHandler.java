package com.tarzan.maxkb4j.module.application.chatpipeline.handler;

import com.tarzan.maxkb4j.module.application.dto.ChatInfo;
import com.tarzan.maxkb4j.module.application.chatpipeline.PipelineManage;

public abstract class PostResponseHandler {

    public abstract void handler(ChatInfo chatInfo, String chatId, String chatRecordId,String problemText, String answerText, PipelineManage manage, String clientId);
}
