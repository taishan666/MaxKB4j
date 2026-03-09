package com.maxkb4j.core.chat.service;

import com.maxkb4j.application.vo.ApplicationVO;
import com.maxkb4j.chat.vo.ChatMessageVO;
import com.maxkb4j.application.dto.ChatParams;
import com.maxkb4j.chat.vo.ChatResponse;
import reactor.core.publisher.Sinks;

public interface IChatService {

    ChatResponse chatMessage(ApplicationVO application, ChatParams chatParams, Sinks.Many<ChatMessageVO> sink);
}
