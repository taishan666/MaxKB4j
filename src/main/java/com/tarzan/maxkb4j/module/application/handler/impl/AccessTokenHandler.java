package com.tarzan.maxkb4j.module.application.handler.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.tarzan.maxkb4j.common.exception.ApiException;
import com.tarzan.maxkb4j.common.util.WebUtil;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationAccessTokenEntity;
import com.tarzan.maxkb4j.module.application.enums.ChatUserType;
import com.tarzan.maxkb4j.module.application.handler.AuthHandler;
import com.tarzan.maxkb4j.module.application.service.ApplicationAccessTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccessTokenHandler implements AuthHandler {

    private final ApplicationAccessTokenService accessTokenService;

    @Override
    public boolean handle(HttpServletResponse response) {
        String tokenValue = WebUtil.getTokenValue();
        StpUtil.setTokenValue(tokenValue);
        String accessToken = (String) StpUtil.getExtra("accessToken");
        ApplicationAccessTokenEntity token = accessTokenService.getByToken(accessToken);
        if (token == null || !token.getIsActive()) {
            throw new ApiException("accessToken不合法或被禁用");
        }
        return true;
    }

    @Override
    public boolean support(HttpServletRequest request) {
        String tokenValue = WebUtil.getTokenValue();
        StpUtil.setTokenValue(tokenValue);
        String chatUserType = (String) StpUtil.getExtra("chatUserType");
        return ChatUserType.ANONYMOUS_USER.name().equals(chatUserType);
    }
}
