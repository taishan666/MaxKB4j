package com.tarzan.maxkb4j.core.chat.provider;

import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.chat.dto.ChatParams;
import com.tarzan.maxkb4j.module.chat.dto.ChatResponse;
import reactor.core.publisher.Sinks;

public interface IChatActuator {

    ChatResponse chatMessage(ApplicationVO application, ChatParams chatParams, Sinks.Many<ChatMessageVO> sink);
}
