package com.tarzan.maxkb4j.core.chat.provider;

import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.chat.ChatParams;

public interface IChatActuator {

    String chatMessage(ApplicationVO application,ChatParams chatParams);
}
