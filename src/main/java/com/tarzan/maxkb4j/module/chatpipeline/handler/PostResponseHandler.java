package com.tarzan.maxkb4j.module.chatpipeline.handler;

import com.tarzan.maxkb4j.module.chatpipeline.ChatInfo;
import com.tarzan.maxkb4j.module.chatpipeline.PipelineManage;

import java.util.UUID;

public abstract class PostResponseHandler {

    public abstract void handler(ChatInfo chatInfo, UUID chatId, UUID chatRecordId,String problemText, String answerText, PipelineManage manage, UUID clientId);
}
