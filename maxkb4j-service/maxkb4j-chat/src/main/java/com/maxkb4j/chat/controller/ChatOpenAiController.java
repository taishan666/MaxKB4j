package com.maxkb4j.chat.controller;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.maxkb4j.application.dto.ApplicationApiKeyDTO;
import com.maxkb4j.application.dto.ChatResponse;
import com.maxkb4j.application.dto.ResultCallback;
import com.maxkb4j.application.service.IApplicationApiKeyService;
import com.maxkb4j.application.service.IApplicationChatService;
import com.maxkb4j.chat.dto.OpenAIChatCompletionRequest;
import com.maxkb4j.chat.dto.OpenAIChatCompletionResponse;
import com.maxkb4j.chat.dto.OpenAIMessage;
import com.maxkb4j.common.cache.ChatCache;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.domain.dto.ChatInfo;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatRecordDTO;
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
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Tag(name = "MaxKB4J兼容 OpenAI API 格式")
@RestController
@RequestMapping(AppConst.CHAT_API)
@RequiredArgsConstructor
@Slf4j
public class ChatOpenAiController {

    private static final String DEFAULT_MODEL_NAME = "gpt-5.4";
    private static final String DONE_SIGNAL = "[DONE]";
    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    /**
     * 相邻信号间的空闲超时：超时后流按错误收尾，避免连接无限挂起。
     */
    private static final Duration STREAM_IDLE_TIMEOUT = Duration.ofMinutes(10);

    private final IApplicationChatService chatService;

    private final IApplicationApiKeyService apiKeyService;

    /**
     * 取 messages 中最后一个 user 消息之前的 user/assistant 轮次，转换为内部聊天记录；
     * 末尾的 user 消息为当前问题，不纳入历史；system 消息不进入对话历史。
     */
    static List<ChatRecordDTO> buildHistoryRecords(List<OpenAIMessage> messages) {
        List<ChatRecordDTO> history = new ArrayList<>();
        if (messages == null) {
            return history;
        }
        OpenAIMessage pendingUserMessage = null;
        for (OpenAIMessage message : messages) {
            if (message == null) {
                continue;
            }
            if (ROLE_USER.equals(message.getRole())) {
                // 上一条 user 未得到回答：按空回答落一条记录，保持轮次完整
                if (pendingUserMessage != null) {
                    history.add(newRecord(pendingUserMessage, null));
                }
                pendingUserMessage = message;
            } else if (ROLE_ASSISTANT.equals(message.getRole()) && pendingUserMessage != null) {
                history.add(newRecord(pendingUserMessage, message));
                pendingUserMessage = null;
            }
        }
        // 循环结束后仍挂起的 user 消息即当前问题，不纳入历史
        return history;
    }

    private static ChatRecordDTO newRecord(OpenAIMessage problem, OpenAIMessage answer) {
        ChatRecordDTO record = new ChatRecordDTO();
        record.setProblemText(problem.getContent());
        record.setAnswerText(answer != null ? answer.getContent() : "");
        return record;
    }

    @Operation(summary = "聊天对话", description = "兼容 OpenAI Chat Completions API 格式")
    @PostMapping(value = "/{appId}/chat/completions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatCompletionStream(@PathVariable String appId, @RequestBody OpenAIChatCompletionRequest request) {
        authenticate(appId);
        PreparedChat prepared = prepareChat(appId, request);
        return handleStreamResponse(request, prepared.params(), prepared.chatState());
    }

    @Operation(summary = "聊天对话", description = "兼容 OpenAI Chat Completions API 格式")
    @PostMapping(value = "/{appId}/chat/completions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> chatCompletionSync(@PathVariable String appId, @RequestBody OpenAIChatCompletionRequest request) {
        authenticate(appId);
        PreparedChat prepared = prepareChat(appId, request);
        return handleSyncResponse(request, prepared.params(), prepared.chatState());
    }

    /**
     * 通过 Authorization Bearer secretKey 认证；API Key 按应用签发，
     * 须与路径中的 appId 匹配，防止跨应用越权调用。
     */
    private void authenticate(String appId) {
        String secretKey = WebUtil.getTokenValue();
        ApplicationApiKeyDTO apiKey = apiKeyService.getBySecretKey(secretKey);
        if (apiKey == null || !Boolean.TRUE.equals(apiKey.getIsActive())
                || !Objects.equals(apiKey.getApplicationId(), appId)) {
            throw new ApiException("chat.token.invalid.or.disabled");
        }
    }

    /**
     * Prepare the shared request context: open a chat session, seed conversation history,
     * and build ChatParams / ChatState used by the business execution.
     */
    private PreparedChat prepareChat(String appId, OpenAIChatCompletionRequest request) {
        String chatId = chatService.chatOpen(appId, false);
        seedConversationHistory(chatId, request);
        ChatParams params = convertToChatParams(request, chatId);
        ChatState chatState = ChatState.builder()
                .appId(appId)
                .chatUserId(IdWorker.get32UUID())
                .chatUserType(ChatUserType.APPLICATION_API_KEY)
                .source(ChatSource.API_CALL)
                .ipAddress(WebUtil.getIP())
                .debug(false)
                .build();
        return new PreparedChat(params, chatState);
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
    private Flux<ServerSentEvent<String>> handleStreamResponse(OpenAIChatCompletionRequest request, ChatParams params, ChatState chatState) {
        String completionId = generateCompletionId();
        String model = resolveModelName(request);
        Sinks.Many<ChatMessageVO> sink = Sinks.many().unicast().onBackpressureBuffer();
        // 异步执行业务逻辑；订阅前的消息由 sink 缓冲
        chatService.chatMessageAsync(params, chatState, buildSinkCallback(sink));

        return sink.asFlux()
                .timeout(STREAM_IDLE_TIMEOUT)
                .map(chatMessage -> toChunkEvent(completionId, model, chatMessage))
                .concatWithValues(doneEvent())
                .doOnError(error -> log.error("OpenAI 兼容接口流式响应异常", error))
                .onErrorResume(e -> Flux.just(errorEvent(e), doneEvent()));
    }

    private ResultCallback<ChatMessageVO> buildSinkCallback(Sinks.Many<ChatMessageVO> sink) {
        return new ResultCallback<>() {
            @Override
            public void onEvent(ChatMessageVO message) {
                sink.tryEmitNext(message);
            }

            @Override
            public void onComplete() {
                sink.tryEmitComplete();
            }

            @Override
            public void onError(Throwable e) {
                sink.tryEmitError(e);
            }
        };
    }

    /**
     * 处理同步响应
     */
    private ResponseEntity<String> handleSyncResponse(OpenAIChatCompletionRequest request, ChatParams params, ChatState chatState) {
        ChatResponse chatResponse = chatService.chatMessage(params, chatState, null);
        OpenAIChatCompletionResponse response = OpenAIChatCompletionResponse.createCompletion(
                generateCompletionId(),
                resolveModelName(request),
                chatResponse.getAnswer(),
                chatResponse.getMessageTokens(),
                chatResponse.getAnswerTokens()
        );
        // 与流式路径一致使用 fastjson 序列化，保证 finish_reason / prompt_tokens 等
        // snake_case 字段输出符合 OpenAI 规范（Spring 默认的 Jackson 不识别 @JSONField）
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(toJson(response));
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
        if (chatInfo == null) {
            log.warn("会话缓存未命中，丢弃请求携带的历史消息 chatId={}", chatId);
            return;
        }
        // ChatInfo.chatRecordList 供流水线异步线程并发读写，必须保持 CopyOnWriteArrayList，
        // 不能整体 set 覆盖；缓存持有的是同一引用，追加后无需重新 put
        List<ChatRecordDTO> chatRecordList = chatInfo.getChatRecordList();
        if (chatRecordList != null) {
            chatRecordList.addAll(history);
        } else {
            chatInfo.setChatRecordList(new CopyOnWriteArrayList<>(history));
        }
    }

    private ServerSentEvent<String> toChunkEvent(String completionId, String model, ChatMessageVO chatMessage) {
        OpenAIChatCompletionResponse chunk = OpenAIChatCompletionResponse.createChunk(
                completionId,
                model,
                0,
                chatMessage.getContent(),
                Boolean.TRUE.equals(chatMessage.getIsEnd()) ? "stop" : null
        );
        return sse(toJson(chunk));
    }

    private ServerSentEvent<String> errorEvent(Throwable e) {
        return sse(toJson(Map.of(
                "error", Map.of(
                        "message", buildErrorMessage(e),
                        "type", "server_error"
                )
        )));
    }

    private ServerSentEvent<String> doneEvent() {
        return sse(DONE_SIGNAL);
    }

    private ServerSentEvent<String> sse(String data) {
        return ServerSentEvent.<String>builder().data(data).build();
    }

    private String resolveModelName(OpenAIChatCompletionRequest request) {
        return StringUtils.isNotBlank(request.getModel()) ? request.getModel() : DEFAULT_MODEL_NAME;
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
    private record PreparedChat(ChatParams params, ChatState chatState) {
    }
}
