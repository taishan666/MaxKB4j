package com.maxkb4j.application.service;

import com.maxkb4j.application.dto.ChatResponse;
import com.maxkb4j.common.domain.vo.ResultCallback;
import com.maxkb4j.application.vo.ApplicationVO;
import com.maxkb4j.common.domain.vo.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatState;

public interface IChatService {

    ChatResponse chatMessage(ApplicationVO application, ChatParams chatParams, ChatState chatState, ResultCallback<ChatMessageVO> callback);
}
