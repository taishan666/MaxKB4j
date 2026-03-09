package com.maxkb4j.application.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.application.entity.ApplicationChatEntity;
import com.maxkb4j.application.dto.ChatParams;
import com.maxkb4j.chat.vo.ChatMessageVO;
import com.maxkb4j.chat.vo.ChatResponse;
import reactor.core.publisher.Sinks;

import java.util.concurrent.CompletableFuture;

public interface IApplicationChatService extends IService<ApplicationChatEntity> {

    String chatOpen(String appId, boolean debug);

    ChatResponse chatMessage(ChatParams chatParams, Sinks.Many<ChatMessageVO> sink);
    CompletableFuture<ChatResponse> chatMessageAsync(ChatParams chatParams, Sinks.Many<ChatMessageVO> sink);
    Boolean deleteById(String chatId);
}
