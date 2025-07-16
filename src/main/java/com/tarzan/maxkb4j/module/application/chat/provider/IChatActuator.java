package com.tarzan.maxkb4j.module.application.chat.provider;

import com.tarzan.maxkb4j.module.application.domian.dto.ChatMessageDTO;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationEntity;

public interface IChatActuator {

    String chatOpen(ApplicationEntity application, String chatId);

    String chatMessage(ChatMessageDTO dto);
}
