package com.tarzan.maxkb4j.module.application.handler;

import com.alibaba.fastjson.JSONObject;

public abstract class PostResponseHandler {

    public abstract void handler(String chatId, String chatRecordId, String problemText, String answerText, JSONObject details, long startTime, String chatUserId, String chatUserType,boolean debug);
}
