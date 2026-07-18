package com.maxkb4j.application.service;

import com.maxkb4j.application.vo.ApplicationVO;
import com.maxkb4j.common.domain.dto.ChatContext;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatResponse;
import reactor.core.publisher.Sinks;

public interface IChatService {

    ChatResponse chatMessage(ApplicationVO application, ChatParams chatParams, ChatContext chatContext, Sinks.Many<ChatMessageVO> sink);
}
