package com.maxkb4j.chat.controller;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.application.dto.ApplicationApiKeyDTO;
import com.maxkb4j.application.dto.ChatResponse;
import com.maxkb4j.application.service.IApplicationApiKeyService;
import com.maxkb4j.application.service.IApplicationChatService;
import com.maxkb4j.chat.filter.ChatCompletionsStreamRoutingFilter;
import com.maxkb4j.common.domain.dto.Answer;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
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
import reactor.core.publisher.Sinks;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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
        doAnswer(invocation -> {
                    Sinks.Many<ChatMessageVO> sink = invocation.getArgument(2);
                    sink.tryEmitError(new RateLimitException("rate limit hit"));
                    return null;
                })
                .when(chatService).chatMessageAsync(any(ChatParams.class), any(ChatState.class), any());
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
        doAnswer(invocation -> {
                    Sinks.Many<ChatMessageVO> sink = invocation.getArgument(2);
                    sink.tryEmitError(new RateLimitException("rate limit hit"));
                    return null;
                })
                .when(chatService).chatMessageAsync(any(ChatParams.class), any(ChatState.class), any());

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
        assertTrue(body.contains("rate limit hit"), body);
        assertFalse(body.contains("Unrecognized Type"), body);
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

    private static ApplicationApiKeyDTO validApiKey() {
        ApplicationApiKeyDTO apiKey = new ApplicationApiKeyDTO();
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
}
