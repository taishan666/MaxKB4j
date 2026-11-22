package com.maxkb4j.chat.controller;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.application.dto.ApplicationApiKeyDTO;
import com.maxkb4j.application.dto.ChatResponse;
import com.maxkb4j.common.domain.vo.ResultCallback;
import com.maxkb4j.application.service.IApplicationApiKeyService;
import com.maxkb4j.application.service.IApplicationChatService;
import com.maxkb4j.chat.filter.ChatCompletionsStreamRoutingFilter;
import com.maxkb4j.common.domain.vo.Answer;
import com.maxkb4j.common.domain.vo.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.common.handler.GlobalExceptionHandler;
import dev.langchain4j.exception.RateLimitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;
import com.maxkb4j.common.enums.ChatUserType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

/**
 * Regression tests for the OpenAI-compatible chat endpoint.
 *
 * <p>Routing between the SSE and the JSON handler is driven by the {@code stream}
 * field of the request body via {@link ChatCompletionsStreamRoutingFilter}, not by
 * the client-side {@code Accept} header. The endpoint used to declare {@code Object}
 * as the return type; with an async exception, Spring MVC rewrote the response using
 * an unresolved generic type and the body failed with
 * "Unrecognized Type: ResolvableType$EmptyType". Handlers now use concrete types.</p>
 */
class ChatOpenAiControllerTest {
    private MockMvc mvc;
    private IApplicationChatService chatService;
    private IApplicationApiKeyService apiKeyService;

    private static ApplicationApiKeyDTO validApiKey() {
        ApplicationApiKeyDTO apiKey = new ApplicationApiKeyDTO();
        apiKey.setId("api-key-1");
        apiKey.setIsActive(true);
        apiKey.setApplicationId("app-1");
        return apiKey;
    }

    private static ChatResponse sampleChatResponse() {
        return new ChatResponse(
                List.of(Answer.builder().content("hello world").build()),
                new JSONObject());
    }

    private static String body(boolean stream) {
        return "{\"model\":\"test-model\",\"stream\":" + stream
                + ",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}";
    }

    @BeforeEach
    void setUp() {
        chatService = mock(IApplicationChatService.class);
        apiKeyService = mock(IApplicationApiKeyService.class);
        when(apiKeyService.getBySecretKey(any())).thenReturn(validApiKey());
        when(chatService.chatOpen(anyString(), anyBoolean())).thenReturn("chat-1");
        mvc = MockMvcBuilders
                .standaloneSetup(new ChatOpenAiController(chatService, apiKeyService))
                .addFilters(new ChatCompletionsStreamRoutingFilter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void streamTrue_wildcardAccept_routesToSse() throws Exception {
        when(chatService.chatMessageAsync(any(ChatParams.class), any(ChatState.class), any()))
                .thenAnswer(invocation -> {
                    ResultCallback<ChatMessageVO> callback = invocation.getArgument(2);
                    callback.onError(new RateLimitException("rate limit hit"));
                    return CompletableFuture.completedFuture(null);
                });
        MvcResult sync = mvc.perform(post("/chat/api/app-1/chat/completions")
                        .accept(MediaType.ALL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(true)))
                .andExpect(request().asyncStarted())
                .andReturn();
        MvcResult result = mvc.perform(asyncDispatch(sync)).andReturn();
        String body = result.getResponse().getContentAsString();
        assertTrue(result.getResponse().getStatus() == 200, "status=" + result.getResponse().getStatus());
        assertTrue(body.contains("data:"), body);
        assertTrue(body.contains("[DONE]"), body);
        assertTrue(body.contains("rate limit hit"), body);
        assertFalse(body.contains("Unrecognized Type"), body);
    }

    @Test
    void streamFalse_wildcardAccept_routesToJson() throws Exception {
        when(chatService.chatMessage(any(ChatParams.class), any(ChatState.class), any())).thenReturn(sampleChatResponse());

        MvcResult result = mvc.perform(post("/chat/api/app-1/chat/completions")
                        .accept(MediaType.ALL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(false)))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertTrue(result.getResponse().getStatus() == 200, "status=" + result.getResponse().getStatus());
        assertTrue(body.contains("chat.completion"), body);
        assertTrue(body.contains("hello world"), body);
        assertFalse(body.contains("Unrecognized Type"), body);
    }

    @Test
    void streamTrue_conflictingJsonAccept_stillStreams() throws Exception {
        when(chatService.chatMessageAsync(any(ChatParams.class), any(ChatState.class), any()))
                .thenAnswer(invocation -> {
                    ResultCallback<ChatMessageVO> callback = invocation.getArgument(2);
                    callback.onError(new RateLimitException("rate limit hit"));
                    return CompletableFuture.completedFuture(null);
                });

        MvcResult sync = mvc.perform(post("/chat/api/app-1/chat/completions")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(true)))
                .andExpect(request().asyncStarted())
                .andReturn();
        MvcResult result = mvc.perform(asyncDispatch(sync)).andReturn();
        String body = result.getResponse().getContentAsString();
        assertTrue(result.getResponse().getStatus() == 200, "status=" + result.getResponse().getStatus());
        assertTrue(body.contains("data:"), body);
        assertTrue(body.contains("[DONE]"), body);
        assertFalse(body.contains("Unrecognized Type"), body);
    }

    @Test
    void streamFalse_conflictingEventStreamAccept_stillJson() throws Exception {
        when(chatService.chatMessage(any(ChatParams.class), any(ChatState.class), any())).thenReturn(sampleChatResponse());

        MvcResult result = mvc.perform(post("/chat/api/app-1/chat/completions")
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(false)))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertTrue(result.getResponse().getStatus() == 200, "status=" + result.getResponse().getStatus());
        assertTrue(body.contains("chat.completion"), body);
        assertFalse(body.contains("Unrecognized Type"), body);
    }

    @Test
    void streamFalse_rateLimitException_isHandledByGlobalAdvice() throws Exception {
        when(chatService.chatMessage(any(ChatParams.class), any(ChatState.class), any()))
                .thenThrow(new RateLimitException("rate limit hit"));

        MvcResult result = mvc.perform(post("/chat/api/app-1/chat/completions")
                        .accept(MediaType.ALL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(false)))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertTrue(result.getResponse().getStatus() == 200, "status=" + result.getResponse().getStatus());
        assertTrue(body.contains("rate.limit.exceeded"), body);
        assertFalse(body.contains("Unrecognized Type"), body);
    }

    /**
     * chatUserId 必须取 API Key 实体 id（稳定身份）：同一 Key 的调用共享同一统计行，
     * accessNum 每日上限才能对 API 调用生效；若每次调用随机生成，统计表会每次新增一行
     * （INSERT ... ON CONFLICT 恒为 firstVisit）且客户数趋势把每个请求算成新客户。
     */
    @Test
    void chatStateCarriesStableApiKeyUserId() throws Exception {
        ArgumentCaptor<ChatState> captor = ArgumentCaptor.forClass(ChatState.class);
        when(chatService.chatMessage(any(ChatParams.class), any(ChatState.class), any()))
                .thenReturn(sampleChatResponse());

        mvc.perform(post("/chat/api/app-1/chat/completions")
                        .accept(MediaType.ALL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(false)))
                .andReturn();

        verify(chatService).chatMessage(any(ChatParams.class), captor.capture(), any());
        assertEquals("api-key-1", captor.getValue().getChatUserId());
        assertEquals(ChatUserType.APPLICATION_API_KEY, captor.getValue().getChatUserType());
    }

    @Test
    void invalidApiKey_rejectsRequest() throws Exception {
        when(apiKeyService.getBySecretKey(any())).thenReturn(null);

        MvcResult result = mvc.perform(post("/chat/api/app-1/chat/completions")
                        .accept(MediaType.ALL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(false)))
                .andReturn();
        String responseBody = result.getResponse().getContentAsString();
        assertTrue(responseBody.contains("chat.token.invalid.or.disabled"), responseBody);
        assertFalse(responseBody.contains("chat.completion"), responseBody);
    }

    /**
     * messages 无 user 角色时，手工构建的 ChatParams 会绕过 @NotBlank 使 message=null
     * 穿透到持久化层（ChatPostHandler problemText.length() NPE）；必须在入口以 400 拒绝，
     * 且不打开会话、不进入对话流水线。
     */
    @Test
    void messagesWithoutUserRole_rejectedWithoutOpeningChat() throws Exception {
        String noUserRole = "{\"model\":\"test-model\",\"stream\":false,"
                + "\"messages\":[{\"role\":\"system\",\"content\":\"you are helpful\"},"
                + "{\"role\":\"assistant\",\"content\":\"hello\"}]}";

        MvcResult result = mvc.perform(post("/chat/api/app-1/chat/completions")
                        .accept(MediaType.ALL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(noUserRole))
                .andReturn();
        String responseBody = result.getResponse().getContentAsString();
        assertTrue(responseBody.contains("chat.message.required"), responseBody);
        assertFalse(responseBody.contains("chat.completion"), responseBody);
        verify(chatService, never()).chatOpen(anyString(), anyBoolean());
        verify(chatService, never()).chatMessage(any(ChatParams.class), any(ChatState.class), any());
    }

    @Test
    void userMessageWithBlankContent_rejectedWithoutOpeningChat() throws Exception {
        String blankUserContent = "{\"model\":\"test-model\",\"stream\":false,"
                + "\"messages\":[{\"role\":\"user\",\"content\":\"   \"}]}";

        MvcResult result = mvc.perform(post("/chat/api/app-1/chat/completions")
                        .accept(MediaType.ALL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blankUserContent))
                .andReturn();
        String responseBody = result.getResponse().getContentAsString();
        assertTrue(responseBody.contains("chat.message.required"), responseBody);
        assertFalse(responseBody.contains("chat.completion"), responseBody);
        verify(chatService, never()).chatOpen(anyString(), anyBoolean());
        verify(chatService, never()).chatMessage(any(ChatParams.class), any(ChatState.class), any());
    }
}
