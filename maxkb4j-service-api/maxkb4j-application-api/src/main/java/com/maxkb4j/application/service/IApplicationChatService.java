package com.maxkb4j.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.maxkb4j.application.dto.ApplicationChatDTO;
import com.maxkb4j.application.dto.ShareChatDTO;
import com.maxkb4j.application.vo.ShareChatVO;
import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.application.dto.ChatResponse;
import reactor.core.publisher.Sinks;

import java.util.Map;

public interface IApplicationChatService {

    String chatOpen(String appId, boolean debug);

    ChatResponse chatMessage(ChatParams chatParams, ChatState chatContext, Sinks.Many<ChatMessageVO> sink);
    void chatMessageAsync(ChatParams chatParams, ChatState chatContext, Sinks.Many<ChatMessageVO> sink);
    Boolean deleteById(String chatId);

    IPage<ApplicationChatDTO> page(String appId, String userId, int current, int size);
    boolean clear(String appId, String userId);
    boolean updateDtoById(ApplicationChatDTO applicationChatDTO);


    Map<String, String> shareChat(String id, String chatId, ShareChatDTO dto);
    ShareChatVO shareChat(String id);
}
