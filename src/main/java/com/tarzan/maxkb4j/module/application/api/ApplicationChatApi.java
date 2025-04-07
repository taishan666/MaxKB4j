package com.tarzan.maxkb4j.module.application.api;

import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.module.application.dto.ChatMessageDTO;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatService;
import com.tarzan.maxkb4j.module.application.vo.ChatMessageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * @author tarzan
 * @date 2024-12-25 13:09:54
 */
@Tag(name = "APP会话管理", description = "APP会话管理")
@RestController
@AllArgsConstructor
public class ApplicationChatApi {

    private final ApplicationChatService chatService;

    @Operation(summary = "新建应用会话", description = "")
    @GetMapping("api/application/{appId}/chat/open")
    public R<String> chatOpen(@PathVariable("appId") String appId) {
        return R.success(chatService.chatOpen(appId));
    }

    @Operation(summary = "聊天", description = "")
    @PostMapping(path = "api/application/chat_message/{chatId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatMessageVO> chatMessage(@PathVariable String chatId, @RequestBody ChatMessageDTO params) {
        return chatService.chatMessage(chatId, params);
    }

}
