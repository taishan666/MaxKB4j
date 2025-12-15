package com.tarzan.maxkb4j.module.application.controller;

import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.common.util.StpKit;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.application.enums.ChatUserType;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatService;
import com.tarzan.maxkb4j.module.chat.dto.ChatParams;
import lombok.AllArgsConstructor;
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
@RequiredArgsConstructor
@RequestMapping(AppConst.ADMIN_API)
public class ChatMessageController {

    private final ApplicationChatService chatService;
    private final TaskExecutor chatTaskExecutor;

    @GetMapping("/workspace/default/application/{appId}/open")
    public R<String> open(@PathVariable("appId") String appId) {
        return R.success(chatService.chatOpen(appId,true));
    }

    @PostMapping(path = "/chat_message/{chatId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatMessageVO> chatMessage(@PathVariable String chatId, @RequestBody ChatParams params) {
        Sinks.Many<ChatMessageVO> sink = Sinks.many().unicast().onBackpressureBuffer();
        params.setChatId(chatId);
        params.setChatUserId(StpKit.ADMIN.getLoginIdAsString());
        params.setChatUserType(ChatUserType.ANONYMOUS_USER.name());
        params.setDebug(true);
        // 异步执行业务逻辑
        chatTaskExecutor.execute(() -> chatService.chatMessage(params,sink));
        return sink.asFlux();
    }
}
