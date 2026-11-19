package com.maxkb4j.chat.handler.impl;

import com.maxkb4j.application.service.IApplicationAccessTokenService;
import com.maxkb4j.chat.handler.AuthHandler;
import com.maxkb4j.common.enums.ChatUserType;
import com.maxkb4j.common.util.StpKit;
import com.maxkb4j.common.util.WebUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserTokenHandler implements AuthHandler {

    private final IApplicationAccessTokenService accessTokenService;

    @Override
    public boolean handle(HttpServletResponse response) {
        String userId = StpKit.USER.getLoginIdAsString();
        //todo
        return true;
    }

    @Override
    public boolean support(HttpServletRequest request) {
        String tokenValue = WebUtil.getTokenValue(request);
        if (Objects.isNull(tokenValue)) {
            return false;
        }
        StpKit.USER.setTokenValue(tokenValue);
        String chatUserType = (String) StpKit.USER.getExtra("chatUserType");
        return ChatUserType.CHAT_USER.getKey().equals(chatUserType);
    }
}
