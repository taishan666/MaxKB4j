package com.tarzan.maxkb4j.core.chat.provider;

import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.chat.ChatParams;

public interface IChatActuator {

    String chatOpenTest(ApplicationVO application);

    String chatOpen(ApplicationVO application, String chatId);

    String chatMessage(ChatParams chatParams,boolean debug);
}
