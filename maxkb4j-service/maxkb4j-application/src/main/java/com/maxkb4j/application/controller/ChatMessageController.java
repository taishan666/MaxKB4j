package com.maxkb4j.application.controller;

import com.maxkb4j.application.service.IApplicationChatInternalService;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.annotation.CurrentUserId;
import com.maxkb4j.common.enums.ChatSource;
import com.maxkb4j.common.enums.ChatUserType;
import com.maxkb4j.common.util.WebUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;


/**
 * @author tarzan
 * @date 2024-12-25 13:09:54
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(AppConst.ADMIN_API)
public class ChatMessageController {

    private final IApplicationChatInternalService chatService;

    @GetMapping("/workspace/default/application/{id}/open")
    public R<String> open(@PathVariable("id") String id) {
        return R.data(chatService.chatOpen(id, true));
    }

    @PostMapping(path = "/chat_message/{chatId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatMessageVO> chatMessage(@PathVariable String chatId, @RequestBody ChatParams params, @CurrentUserId String userId) {
        Sinks.Many<ChatMessageVO> sink = Sinks.many().unicast().onBackpressureBuffer();
        params.setChatId(chatId);
        ChatState chatState = ChatState.builder()
                .chatUserId(userId)
                .chatUserType(ChatUserType.PLATFORM_USER)
                .source(ChatSource.ONLINE)
                .ipAddress(WebUtil.getIP())
                .debug(true)
                .build();
        // 异步执行业务逻辑
        chatService.chatMessageAsync(params, chatState, sink);
        return sink.asFlux();
    }
}
