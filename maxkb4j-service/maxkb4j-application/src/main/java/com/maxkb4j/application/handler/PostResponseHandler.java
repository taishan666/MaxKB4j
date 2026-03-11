package com.maxkb4j.application.handler;


import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatResponse;

public interface PostResponseHandler {

    void handler(ChatParams chatParams, ChatResponse chatResponse, long startTime);
}
