package com.tarzan.maxkb4j.module.application.chatpipeline.handler;

import com.tarzan.maxkb4j.module.application.chatpipeline.ChatInfo;
import com.tarzan.maxkb4j.module.application.chatpipeline.PipelineManage;

import java.util.UUID;

public abstract class PostResponseHandler {

    public abstract void handler(ChatInfo chatInfo, UUID chatId, UUID chatRecordId,String problemText, String answerText, PipelineManage manage, UUID clientId);
}
