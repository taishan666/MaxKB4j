package com.tarzan.maxkb4j.module.application.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.tarzan.maxkb4j.constant.AppConst;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatMessageDTO;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;


/**
 * @author tarzan
 * @date 2024-12-25 13:09:54
 */
@RestController
@RequestMapping(AppConst.ADMIN_API)
public class ChatMessageController {

    private final ApplicationChatService chatService;
    private final TaskExecutor chatTaskExecutor;

    public ChatMessageController(ApplicationChatService chatService, @Qualifier("chatTaskExecutor") TaskExecutor chatTaskExecutor) {
        this.chatService = chatService;
        this.chatTaskExecutor = chatTaskExecutor;
    }

    @PostMapping(path = "/chat_message/{chatId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatMessageVO> chatMessage(@PathVariable String chatId, @RequestBody ChatMessageDTO params) {
        Sinks.Many<ChatMessageVO> sink = Sinks.many().multicast().onBackpressureBuffer();
        String clientId = (String) StpUtil.getExtra("chat_user_id");
        String clientType = (String) StpUtil.getExtra("chat_user_type");
        params.setChatId(chatId);
        params.setClientId(clientId);
        params.setClientType(clientType);
        params.setSink(sink);
        // 异步执行业务逻辑
        chatTaskExecutor.execute(() -> chatService.chatMessage(params));
        return sink.asFlux();
    }
}
