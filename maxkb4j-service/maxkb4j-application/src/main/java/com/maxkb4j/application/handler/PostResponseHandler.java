package com.maxkb4j.application.handler;


import com.maxkb4j.application.dto.ChatParams;
import com.maxkb4j.chat.vo.ChatResponse;

public interface PostResponseHandler {

    void handler(ChatParams chatParams, ChatResponse chatResponse, long startTime);
}
