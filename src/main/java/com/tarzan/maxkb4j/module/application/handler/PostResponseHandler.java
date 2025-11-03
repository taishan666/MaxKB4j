package com.tarzan.maxkb4j.module.application.handler;

import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.chat.dto.ChatParams;
import com.tarzan.maxkb4j.module.chat.dto.ChatResponse;

public abstract class PostResponseHandler {

    public abstract void handler(ChatParams chatParams, ChatResponse chatResponse, ApplicationChatRecordEntity chatRecord, long startTime);
}
