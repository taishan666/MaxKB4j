package com.tarzan.maxkb4j.module.application.handler;

import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.chat.dto.ChatResponse;

public abstract class PostResponseHandler {

    public abstract void handler(String chatId, String chatRecordId, String problemText, ChatResponse chatResponse, ApplicationChatRecordEntity chatRecord,  long startTime, String chatUserId, String chatUserType, boolean debug);
}
