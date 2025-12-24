package com.tarzan.maxkb4j.module.application.handler.impl;

import com.tarzan.maxkb4j.common.util.ResponseProvider;
import com.tarzan.maxkb4j.common.util.StpKit;
import com.tarzan.maxkb4j.common.util.WebUtil;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationAccessTokenEntity;
import com.tarzan.maxkb4j.module.application.enums.ChatUserType;
import com.tarzan.maxkb4j.module.application.handler.AuthHandler;
import com.tarzan.maxkb4j.module.application.service.ApplicationAccessTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatAnonymousTokenHandler implements AuthHandler {

    private final ApplicationAccessTokenService accessTokenService;

    @Override
    public boolean handle(HttpServletResponse response) {
        String tokenValue = WebUtil.getTokenValue();
        StpKit.USER.setTokenValue(tokenValue);
        String accessToken = (String) StpKit.USER.getExtra("accessToken");
        ApplicationAccessTokenEntity token = accessTokenService.getByAccessToken(accessToken);
        if (token == null || !token.getIsActive()) {
            log.warn("accessToken不合法或被禁用");
            ResponseProvider.write(response);
            return  false;
        }
        return true;
    }

    @Override
    public boolean support(HttpServletRequest request) {
        String tokenValue = WebUtil.getTokenValue(request);
        if (Objects.isNull(tokenValue)){
            return false;
        }
        StpKit.USER.setTokenValue(tokenValue);
        String chatUserType = (String) StpKit.USER.getExtra("chatUserType");
        return ChatUserType.ANONYMOUS_USER.name().equals(chatUserType);
    }
}
