package com.tarzan.maxkb4j.module.application.chat.provider;

import com.tarzan.maxkb4j.module.application.domian.dto.ChatInfo;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatMessageDTO;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;

public interface IChatActuator {

    String chatOpenTest(ApplicationVO application);

    String chatOpen(ApplicationVO application, String chatId);

    String chatMessage(ChatInfo chatInfo,ChatMessageDTO dto);
}
