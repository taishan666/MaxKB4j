package com.maxkb4j.chat.controller;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.maxkb4j.application.dto.ApplicationApiKeyDTO;
import com.maxkb4j.application.dto.ChatResponse;
import com.maxkb4j.application.service.IApplicationApiKeyService;
import com.maxkb4j.application.service.IApplicationChatService;
import com.maxkb4j.chat.dto.OpenAIChatCompletionRequest;
import com.maxkb4j.chat.dto.OpenAIChatCompletionResponse;
import com.maxkb4j.chat.dto.OpenAIMessage;
import com.maxkb4j.common.cache.ChatCache;
import com.maxkb4j.common.domain.dto.ChatInfo;
import com.maxkb4j.common.domain.dto.ChatRecordDTO;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.common.enums.ChatSource;
import com.maxkb4j.common.enums.ChatUserType;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.common.util.WebUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "MaxKB4J兼容 OpenAI API 格式")
@RestController
@RequestMapping(AppConst.CHAT_API)
@RequiredArgsConstructor
@Slf4j
public class ChatOpenAiController {

    private final IApplicationChatService chatService;

    private final IApplicationApiKeyService apiKeyService;

    private final String DEFAULT_MODEL_NAME="gpt-5.4";

    @Operation(summary = "聊天对话", description = "兼容 OpenAI Chat Completions API 格式")
    @PostMapping(value = "/{appId}/chat/completions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatCompletionStream(@PathVariable String appId, @RequestBody OpenAIChatCompletionRequest request) {
        authenticate();
        PreparedChat prepared = prepareChat(appId, request);
        return handleStreamResponse(request, prepared.params(), prepared.chatState(), prepared.sink());
    }

    @Operation(summary = "聊天对话", description = "兼容 OpenAI Chat Completions API 格式")
    @PostMapping(value = "/{appId}/chat/completions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OpenAIChatCompletionResponse> chatCompletionSync(@PathVariable String appId, @RequestBody OpenAIChatCompletionRequest request) {
        authenticate();
        PreparedChat prepared = prepareChat(appId, request);
        return handleSyncResponse(request, prepared.params(), prepared.chatState(), prepared.sink());
    }

    /**
     * Prepare the shared request context: open a chat session, seed conversation history,
     * and build ChatParams / ChatState / the sink used by the business execution.
     */
    private PreparedChat prepareChat(String appId, OpenAIChatCompletionRequest request) {
        String chatId = chatService.chatOpen(appId, false);
        seedConversationHistory(chatId, request);
        Sinks.Many<ChatMessageVO> sink = Sinks.many().unicast().onBackpressureBuffer();
        ChatParams params = convertToChatParams(request, chatId);
        ChatState chatState = ChatState.builder()
                .appId(appId)
                .chatUserId(IdWorker.get32UUID())
                .chatUserType(ChatUserType.APPLICATION_API_KEY)
                .source(ChatSource.API_CALL)
                .ipAddress(WebUtil.getIP())
                .debug(false)
                .build();
        return new PreparedChat(params, chatState, sink);
    }

    /**
     * Authenticate the request via the Authorization Bearer secretKey.
     */
    private void authenticate() {
        String secretKey = WebUtil.getTokenValue();
        ApplicationApiKeyDTO apiKey = apiKeyService.getBySecretKey(secretKey);
        if (apiKey == null || !Boolean.TRUE.equals(apiKey.getIsActive())) {
            throw new ApiException("chat.token.invalid.or.disabled");
        }
    }

    /**
     * 将 OpenAI 请求转换为内部 ChatParams（仅请求入参）
     */
    private ChatParams convertToChatParams(OpenAIChatCompletionRequest request, String chatId) {
        return ChatParams.builder()
                .message(request.getLastUserMessage())
                .chatId(chatId)
                .stream(request.getStream())
                .reChat(false)
                .build();
    }

    /**
     * 处理流式响应
     */
    private Flux<ServerSentEvent<String>> handleStreamResponse(OpenAIChatCompletionRequest request, ChatParams params, ChatState chatState, Sinks.Many<ChatMessageVO> sink) {
        String completionId = generateCompletionId();
        String model = StringUtils.isNotBlank(request.getModel()) ? request.getModel() : DEFAULT_MODEL_NAME;
        // 异步执行业务逻辑
        chatService.chatMessageAsync(params, chatState, sink);

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
                                .data(toJson(Map.of(
                                        "error", Map.of(
                                                "message", buildErrorMessage(e),
                                                "type", "server_error"
                                        )
                                )))
                                .build(),
                        ServerSentEvent.<String>builder()
                                .data("[DONE]")
                                .build()
                ));
    }

    /**
     * 处理同步响应
     */
    private ResponseEntity<OpenAIChatCompletionResponse> handleSyncResponse(OpenAIChatCompletionRequest request, ChatParams params, ChatState chatState, Sinks.Many<ChatMessageVO> sink) {
        ChatResponse chatResponse = chatService.chatMessage(params, chatState, sink);
        String completionId = generateCompletionId();
        String model = StringUtils.isNotBlank(request.getModel()) ? request.getModel() : DEFAULT_MODEL_NAME;

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
     * 将 OpenAI 请求携带的历史消息（当前问题之前的 user/assistant 轮次）预置到会话缓存，
     * 使对话流水线基于完整上下文生成回答，避免多轮上下文丢失。
     */
    private void seedConversationHistory(String chatId, OpenAIChatCompletionRequest request) {
        List<ChatRecordDTO> history = buildHistoryRecords(request.getMessages());
        if (history.isEmpty()) {
            return;
        }
        ChatInfo chatInfo = ChatCache.get(chatId);
        if (chatInfo != null) {
            chatInfo.setChatRecordList(history);
            ChatCache.put(chatId, chatInfo);
        }
    }

    /**
     * 取 messages 中最后一个 user 消息之前的 user/assistant 轮次，转换为内部聊天记录；
     * 末尾的 user 消息为当前问题，不纳入历史；system 消息不进入对话历史。
     */
    static List<ChatRecordDTO> buildHistoryRecords(List<OpenAIMessage> messages) {
        List<ChatRecordDTO> history = new ArrayList<>();
        if (messages == null || messages.size() < 2) {
            return history;
        }
        int lastUserIndex = -1;
        for (int i = messages.size() - 1; i >= 0; i--) {
            OpenAIMessage msg = messages.get(i);
            if (msg != null && "user".equals(msg.getRole())) {
                lastUserIndex = i;
                break;
            }
        }
        if (lastUserIndex <= 0) {
            return history;
        }
        for (int i = 0; i < lastUserIndex; i++) {
            OpenAIMessage msg = messages.get(i);
            if (msg == null || !"user".equals(msg.getRole())) {
                continue;
            }
            String answer = "";
            for (int j = i + 1; j < messages.size(); j++) {
                OpenAIMessage next = messages.get(j);
                if (next != null && "assistant".equals(next.getRole())) {
                    answer = next.getContent();
                    break;
                }
            }
            ChatRecordDTO record = new ChatRecordDTO();
            record.setProblemText(msg.getContent());
            record.setAnswerText(answer);
            history.add(record);
        }
        return history;
    }

    private String buildErrorMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return message == null || message.isBlank() ? "chat request failed" : message;
    }

    private String generateCompletionId() {
        return "chatcmpl-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }

    /**
     * JSON 序列化
     */
    private String toJson(Object obj) {
        return JSON.toJSONString(obj);
    }

    /**
     * Shared context for one chat completion request.
     */
    private record PreparedChat(ChatParams params, ChatState chatState, Sinks.Many<ChatMessageVO> sink) {
    }
}
