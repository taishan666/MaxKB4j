package com.maxkb4j.application.handler;


import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.application.dto.ChatResponse;

public interface PostResponseHandler {

    void handler(ChatParams chatParams, ChatState chatState, ChatResponse chatResponse, long startTime);
}
