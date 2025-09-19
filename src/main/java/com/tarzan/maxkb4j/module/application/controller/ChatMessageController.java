package com.tarzan.maxkb4j.module.application.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.tarzan.maxkb4j.constant.AppConst;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatService;
import com.tarzan.maxkb4j.module.chat.ChatParams;
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
    public Flux<ChatMessageVO> chatMessage(@PathVariable String chatId, @RequestBody ChatParams params) {
        Sinks.Many<ChatMessageVO> sink = Sinks.many().multicast().onBackpressureBuffer();
        params.setChatId(chatId);
        params.setChatRecordId(params.getChatRecordId()==null? IdWorker.get32UUID() :params.getChatRecordId());
        params.setSink(sink);
        params.setUserId(StpUtil.getLoginIdAsString());
        // 异步执行业务逻辑
        chatTaskExecutor.execute(() -> chatService.chatMessage(params,true));
        return sink.asFlux();
    }
}
