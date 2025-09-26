package com.tarzan.maxkb4j.module.application.handler;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import com.tarzan.maxkb4j.common.exception.ApiException;
import com.tarzan.maxkb4j.core.handler.AuthHandler;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationAccessTokenEntity;
import com.tarzan.maxkb4j.module.application.enums.AuthType;
import com.tarzan.maxkb4j.module.application.service.ApplicationAccessTokenService;
import com.tarzan.maxkb4j.common.util.WebUtil;
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
        String accessToken = (String) StpUtil.getStpLogic().getExtra(AuthType.ACCESS_TOKEN.name());
        ApplicationAccessTokenEntity token = accessTokenService.getByToken(accessToken);
        if (token == null || !token.getIsActive()) {
            throw new ApiException("accessToken不合法或被禁用");
        }
        if (token.getWhiteActive() && CollUtil.isNotEmpty(token.getWhiteList())) {
            String clientIP = WebUtil.getIP();
            System.out.println("clientIP:"+clientIP);
            if (!token.getWhiteList().contains(clientIP)) {
                throw new ApiException("非法访问");
            }
        }
        return true;
    }

    @Override
    public boolean support(HttpServletRequest request) {
        String clientType = (String) StpUtil.getExtra("client_type");
        String clientIP = WebUtil.getIP();
        return AuthType.ACCESS_TOKEN.name().equals(clientType)&&!"127.0.0.1".equals(clientIP);
    }
}
