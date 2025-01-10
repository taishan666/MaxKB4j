package com.tarzan.maxkb4j.module.chatpipeline;

import java.util.UUID;

public abstract class PostResponseHandler {

    public abstract void handler(UUID chatId, UUID chatRecordId, String problemText, String answerText, PipelineManage manage, UUID clientId);
}
