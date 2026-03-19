package com.maxkb4j.chat.controller;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.maxkb4j.application.service.*;
import com.maxkb4j.chat.service.ChatApiService;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatResponse;
import com.maxkb4j.common.enums.ChatUserType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Sinks;

@Tag(name = "MaxKB4J开放接口")
@RestController
@RequestMapping(AppConst.CHAT_API)
@RequiredArgsConstructor
public class ChatOpenAiController {

    private final IApplicationChatService chatService;

    @Operation(summary = "聊天对话", description = "聊天对话")
    @PostMapping(path = "/{appId}/v1/chat/completions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    public Object chatMessage(@PathVariable String appId, @RequestBody ChatParams params) {
        String chatId = chatService.chatOpen(appId, false);
        Sinks.Many<ChatMessageVO> sink = Sinks.many().unicast().onBackpressureBuffer();
        params.setChatId(chatId);
        params.setChatUserId(IdWorker.get32UUID());
        params.setChatUserType(ChatUserType.ANONYMOUS_USER.name());
        params.setDebug(false);
        if (Boolean.TRUE.equals(params.getStream())) {
            // 异步执行业务逻辑
            chatService.chatMessageAsync(params, sink);
            return sink.asFlux();
        } else {
            ChatResponse chatResponse = chatService.chatMessage(params, sink);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(R.data(chatResponse));
        }
    }



}
