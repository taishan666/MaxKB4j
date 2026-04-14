package com.maxkb4j.chat.controller;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.maxkb4j.application.service.IApplicationChatService;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.domain.dto.*;
import com.maxkb4j.common.enums.ChatUserType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.UUID;

@Tag(name = "MaxKB4J兼容 OpenAI API 格式")
@RestController
@RequestMapping(AppConst.CHAT_API)
@RequiredArgsConstructor
@Slf4j
public class ChatOpenAiController {

    private final IApplicationChatService chatService;

    @Operation(summary = "聊天对话", description = "兼容 OpenAI Chat Completions API 格式")
    @PostMapping("/{appId}/chat/completions")
    public Object chatCompletion(@PathVariable String appId, @RequestBody OpenAIChatCompletionRequest request) {
        String chatId = chatService.chatOpen(appId, false);
        Sinks.Many<ChatMessageVO> sink = Sinks.many().unicast().onBackpressureBuffer();
        // 构建 ChatParams
        ChatParams params = convertToChatParams(request, chatId, appId);
        if (Boolean.TRUE.equals(request.getStream())) {
            return handleStreamResponse(request, params, sink);
        } else {
            return handleSyncResponse(request, params, sink);
        }
    }

    /**
     * 将 OpenAI 请求转换为内部 ChatParams
     */
    private ChatParams convertToChatParams(OpenAIChatCompletionRequest request, String chatId, String appId) {
        return ChatParams.builder()
                .message(request.getLastUserMessage())
                .chatId(chatId)
                .chatUserId(IdWorker.get32UUID())
                .chatUserType(ChatUserType.ANONYMOUS_USER.name())
                .debug(false)
                .stream(request.getStream())
                .reChat(false)
                .build();
    }

    /**
     * 处理流式响应
     */
    private Flux<ServerSentEvent<String>> handleStreamResponse(OpenAIChatCompletionRequest request, ChatParams params, Sinks.Many<ChatMessageVO> sink) {
        String completionId = generateCompletionId();
        String model = request.getModel() != null ? request.getModel() : "maxkb4j";
        // 异步执行业务逻辑
        chatService.chatMessageAsync(params, sink);

        return sink.asFlux()
                .timeout(Duration.ofMinutes(10))
                .map(chatMessage -> {
                    OpenAIChatCompletionResponse chunk = OpenAIChatCompletionResponse.createChunk(
                            completionId,
                            model,
                            0,
                            chatMessage.getContent(),
                            Boolean.TRUE.equals(chatMessage.getIsEnd()) ? "stop" : null
                    );
                    return ServerSentEvent.<String>builder()
                            .data(toJson(chunk))
                            .build();
                })
                .concatWith(Flux.just(
                        ServerSentEvent.<String>builder()
                                .data("[DONE]")
                                .build()
                ))
                .doOnError(error -> log.error("Stream error: {}", error.getMessage(), error))
                .onErrorResume(e -> Flux.just(
                        ServerSentEvent.<String>builder()
                                .data("[DONE]")
                                .build()
                ));
    }

    /**
     * 处理同步响应
     */
    private ResponseEntity<OpenAIChatCompletionResponse> handleSyncResponse(OpenAIChatCompletionRequest request, ChatParams params, Sinks.Many<ChatMessageVO> sink) {
        ChatResponse chatResponse = chatService.chatMessage(params, sink);
        String completionId = generateCompletionId();
        String model = request.getModel() != null ? request.getModel() : "maxkb4j";

        OpenAIChatCompletionResponse response = OpenAIChatCompletionResponse.createCompletion(
                completionId,
                model,
                chatResponse.getAnswer(),
                chatResponse.getMessageTokens(),
                chatResponse.getAnswerTokens()
        );

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    /**
     * 生成 OpenAI 格式的 completion ID
     */
    private String generateCompletionId() {
        return "chatcmpl-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }

    /**
     * JSON 序列化
     */
    private String toJson(Object obj) {
        return JSON.toJSONString(obj);
    }
}