package com.tarzan.maxkb4j.module.application.handler;

import com.tarzan.maxkb4j.module.chat.dto.ChatParams;
import com.tarzan.maxkb4j.module.chat.dto.ChatResponse;

public interface PostResponseHandler {

    void handler(ChatParams chatParams, ChatResponse chatResponse, long startTime);
}
