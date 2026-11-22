package com.maxkb4j.chat.service;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.spring.SaTokenContextForSpringInJakartaServlet;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.application.dto.ApplicationAccessTokenDTO;
import com.maxkb4j.application.dto.ApplicationApiKeyDTO;
import com.maxkb4j.application.dto.ChatResponse;
import com.maxkb4j.application.service.IApplicationAccessTokenService;
import com.maxkb4j.application.service.IApplicationChatRecordService;
import com.maxkb4j.application.service.IApplicationChatService;
import com.maxkb4j.application.service.IApplicationService;
import com.maxkb4j.chat.dto.McpRequest;
import com.maxkb4j.chat.dto.McpResponse;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.common.domain.vo.Answer;
import com.maxkb4j.common.enums.ChatSource;
import com.maxkb4j.common.enums.ChatUserType;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.common.util.StpKit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatApiServiceTest {

    private static final String CLIENT_IP = "203.0.113.10";
    private static final String ACCESS_TOKEN = "access-token";

    private IApplicationAccessTokenService accessTokenService;
    private IApplicationChatService chatService;
    private ChatTokenService chatTokenService;
    private ChatApiService chatApiService;
    private MockHttpServletRequest request;
    private String originalJwtSecretKey;
    private SaTokenContext originalSaTokenContext;

    @BeforeEach
    void setUp() {
        originalJwtSecretKey = SaManager.getConfig().getJwtSecretKey();
        originalSaTokenContext = SaManager.getSaTokenContext();
        SaManager.getConfig().setJwtSecretKey("test-jwt-secret-key");
        SaManager.setSaTokenContext(new SaTokenContextForSpringInJakartaServlet());
        accessTokenService = mock(IApplicationAccessTokenService.class);
        chatTokenService = mock(ChatTokenService.class);
        chatService = mock(IApplicationChatService.class);
        chatApiService = new ChatApiService(
                accessTokenService,
                mock(IApplicationService.class),
                chatService,
                mock(IApplicationChatRecordService.class),
                chatTokenService);
        request = new MockHttpServletRequest();
        request.setRemoteAddr(CLIENT_IP);
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(request, new MockHttpServletResponse()));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SaManager.getConfig().setJwtSecretKey(originalJwtSecretKey);
        SaManager.setSaTokenContext(originalSaTokenContext);
    }

    @Test
    void authTokenRejectsIpOutsideWhitelist() {
        when(accessTokenService.getByAccessToken(ACCESS_TOKEN))
                .thenReturn(accessToken(true, List.of("198.51.100.1")));

        ApiException exception = assertThrows(ApiException.class,
                () -> chatApiService.authToken(authParams()));

        assertEquals("application.access.white.list.required", exception.getMessage());
        verify(chatTokenService, never()).issueAnonymousToken(anyString(), anyMap());
    }

    @Test
    void authTokenAllowsIpInsideWhitelist() {
        when(accessTokenService.getByAccessToken(ACCESS_TOKEN))
                .thenReturn(accessToken(true, List.of(CLIENT_IP)));
        when(chatTokenService.issueAnonymousToken(anyString(), anyMap())).thenReturn("chat-token");

        assertEquals("chat-token", chatApiService.authToken(authParams()));
    }

    @Test
    void authTokenAllowsAnyIpWhenWhitelistDisabled() {
        when(accessTokenService.getByAccessToken(ACCESS_TOKEN))
                .thenReturn(accessToken(false, List.of("198.51.100.1")));
        when(chatTokenService.issueAnonymousToken(anyString(), anyMap())).thenReturn("chat-token");

        assertEquals("chat-token", chatApiService.authToken(authParams()));
    }

    @Test
    void authTokenIgnoresInvalidChatToken() {
        request.addHeader("Authorization", "Bearer invalid-chat-token");
        when(accessTokenService.getByAccessToken(ACCESS_TOKEN))
                .thenReturn(accessToken(false, List.of()));
        when(chatTokenService.issueAnonymousToken(anyString(), anyMap())).thenReturn("chat-token");

        assertEquals("chat-token", chatApiService.authToken(authParams()));
        verify(chatTokenService).issueAnonymousToken(anyString(), anyMap());
    }

    @Test
    void authTokenReusesUserIdFromValidChatToken() {
        String tokenValue = StpKit.USER.createTokenValue(
                "chat-user", null, -1L, Map.of("chatUserType", "ANONYMOUS"));
        request.addHeader("Authorization", "Bearer " + tokenValue);
        when(accessTokenService.getByAccessToken(ACCESS_TOKEN))
                .thenReturn(accessToken(false, List.of()));
        when(chatTokenService.issueAnonymousToken(anyString(), anyMap())).thenReturn("chat-token");

        assertEquals("chat-token", chatApiService.authToken(authParams()));
        verify(chatTokenService).issueAnonymousToken(
                org.mockito.ArgumentMatchers.eq("chat-user"), anyMap());
    }

    /**
     * MCP tools/call 的 chatUserId 必须取 API Key 实体 id（稳定身份）：同一 Key 的调用
     * 共享同一统计行，accessNum 每日上限才能对 MCP 调用生效；随机生成会导致统计表
     * 每次调用新增一行且客户数趋势虚高。
     */
    @Test
    void mcpToolsCallUsesStableApiKeyUserId() {
        when(chatService.chatOpen("app-1", false)).thenReturn("chat-1");
        when(chatService.chatMessage(any(ChatParams.class), any(ChatState.class), any()))
                .thenReturn(new ChatResponse(
                        List.of(Answer.builder().content("ok").build()), new JSONObject()));
        ApplicationApiKeyDTO apiKey = new ApplicationApiKeyDTO();
        apiKey.setId("api-key-1");
        apiKey.setIsActive(true);
        apiKey.setApplicationId("app-1");
        McpRequest req = new McpRequest();
        req.id = "1";
        req.method = "tools/call";
        req.params = new JSONObject(Map.of("arguments", Map.of("message", "hi")));

        McpResponse resp = chatApiService.mcpHandle(apiKey, req);

        ArgumentCaptor<ChatState> captor = ArgumentCaptor.forClass(ChatState.class);
        verify(chatService).chatMessage(any(ChatParams.class), captor.capture(), any());
        assertEquals("api-key-1", captor.getValue().getChatUserId());
        assertEquals(ChatUserType.APPLICATION_API_KEY, captor.getValue().getChatUserType());
        assertEquals(ChatSource.API_CALL, captor.getValue().getSource());
        assertNotNull(resp.result);
    }

    private static ApplicationAccessTokenDTO accessToken(boolean whiteActive, List<String> whiteList) {
        ApplicationAccessTokenDTO token = new ApplicationAccessTokenDTO();
        token.setApplicationId("app-1");
        token.setIsActive(true);
        token.setWhiteActive(whiteActive);
        token.setWhiteList(whiteList);
        return token;
    }

    private static JSONObject authParams() {
        JSONObject params = new JSONObject();
        params.put("accessToken", ACCESS_TOKEN);
        return params;
    }
}
