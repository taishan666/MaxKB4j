package com.tarzan.maxkb4j.module.application.api;

import com.tarzan.maxkb4j.constant.AppConst;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author tarzan
 * @date 2024-12-25 13:09:54
 */
@Tag(name = "应用接口管理", description = "应用接口管理")
@RestController
@AllArgsConstructor
@RequestMapping(AppConst.BASE_PATH)
public class ApplicationChatApi {

    private final ApplicationChatService chatService;

    @Operation(summary = "新建应用会话", description = "新建应用会话")
    @GetMapping("/app/{appId}/v1/open")
    public R<String> chatOpen(@PathVariable("appId") String appId) {
        return R.success(chatService.chatOpen(appId));
    }

  /*  @Operation(summary = "聊天", description = "")
    @PostMapping(path = "api/application/{chatId}/v1/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatMessageVO> chatMessage(@PathVariable String chatId, @RequestBody ChatMessageDTO params) {
        return chatService.chatMessage(chatId, params);
    }*/

}
