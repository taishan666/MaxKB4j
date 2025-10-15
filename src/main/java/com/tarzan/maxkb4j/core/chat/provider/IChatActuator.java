package com.tarzan.maxkb4j.core.chat.provider;

import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.chat.ChatParams;
import com.tarzan.maxkb4j.module.chat.ChatResponse;

public interface IChatActuator {

    ChatResponse chatMessage(ApplicationVO application, ChatParams chatParams);
}
