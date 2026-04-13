package com.maxkb4j.application.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.application.dto.ShareChatDTO;
import com.maxkb4j.application.entity.ApplicationChatEntity;
import com.maxkb4j.application.vo.ShareChatVO;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatResponse;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface IApplicationChatService extends IService<ApplicationChatEntity> {

    String chatOpen(String appId, boolean debug);

    ChatResponse chatMessage(ChatParams chatParams, Sinks.Many<ChatMessageVO> sink);
    CompletableFuture<ChatResponse> chatMessageAsync(ChatParams chatParams, Sinks.Many<ChatMessageVO> sink);
    Boolean deleteById(String chatId);

    Map<String, String> shareChat(String id, String chatId, ShareChatDTO dto);

    ShareChatVO shareChat(String id);
}
