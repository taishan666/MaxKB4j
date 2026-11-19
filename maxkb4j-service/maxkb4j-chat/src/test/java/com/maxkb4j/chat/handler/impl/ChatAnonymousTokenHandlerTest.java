package com.maxkb4j.chat.handler.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.spring.SaTokenContextForSpringInJakartaServlet;
import com.maxkb4j.application.dto.ApplicationAccessTokenDTO;
import com.maxkb4j.application.service.IApplicationAccessTokenService;
import com.maxkb4j.common.util.StpKit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatAnonymousTokenHandlerTest {

    private static final String CLIENT_IP = "203.0.113.10";
    private static final String ACCESS_TOKEN = "access-token";

    private IApplicationAccessTokenService accessTokenService;
    private ChatAnonymousTokenHandler handler;
    private String originalJwtSecretKey;
    private SaTokenContext originalSaTokenContext;

    @BeforeEach
    void setUp() {
        originalJwtSecretKey = SaManager.getConfig().getJwtSecretKey();
        originalSaTokenContext = SaManager.getSaTokenContext();
        SaManager.getConfig().setJwtSecretKey("test-jwt-secret-key");
        SaManager.setSaTokenContext(new SaTokenContextForSpringInJakartaServlet());
        accessTokenService = mock(IApplicationAccessTokenService.class);
        handler = new ChatAnonymousTokenHandler(accessTokenService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(CLIENT_IP);
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(request, new MockHttpServletResponse()));
        String tokenValue = StpKit.USER.createTokenValue(
                "chat-user", null, -1L, Map.of("accessToken", ACCESS_TOKEN));
        StpKit.USER.setTokenValue(tokenValue);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SaManager.getConfig().setJwtSecretKey(originalJwtSecretKey);
        SaManager.setSaTokenContext(originalSaTokenContext);
    }

    @Test
    void handleRejectsIpOutsideWhitelist() {
        when(accessTokenService.getByAccessToken(ACCESS_TOKEN))
                .thenReturn(accessToken(List.of("198.51.100.1")));

        assertFalse(handler.handle(new MockHttpServletResponse()));
    }

    @Test
    void handleAllowsIpInsideWhitelist() {
        when(accessTokenService.getByAccessToken(ACCESS_TOKEN))
                .thenReturn(accessToken(List.of(CLIENT_IP)));

        assertTrue(handler.handle(new MockHttpServletResponse()));
    }

    private static ApplicationAccessTokenDTO accessToken(List<String> whiteList) {
        ApplicationAccessTokenDTO token = new ApplicationAccessTokenDTO();
        token.setIsActive(true);
        token.setWhiteActive(true);
        token.setWhiteList(whiteList);
        return token;
    }
}
