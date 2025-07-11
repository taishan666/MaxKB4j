package com.tarzan.maxkb4j.module.application.handler;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;

public abstract class PostResponseHandler {

    public abstract void handler(String chatId, String chatRecordId, String problemText, String answerText, ApplicationChatRecordEntity chatRecord, JSONObject details, long startTime, String clientId, String clientType);
}
