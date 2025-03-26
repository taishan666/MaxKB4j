package com.tarzan.maxkb4j.core.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.tarzan.maxkb4j.core.handler.AuthHandler;
import com.tarzan.maxkb4j.util.SpringUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collection;


@Slf4j
public class AuthInterceptor implements HandlerInterceptor {

    private static final Collection<AuthHandler> authHandlerList = SpringUtil.getBeansOfType(AuthHandler.class).values();

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws Exception {
        String tokenValue = StpUtil.getStpLogic().getTokenValue(false);
        if (StrUtil.isBlank(tokenValue)){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        AuthHandler authHandler = getAuthHandler(request);
        if (authHandler!=null){
            return authHandler.handle(response);
        }
        return true;
    }

    private AuthHandler getAuthHandler(HttpServletRequest request) {
        for (AuthHandler authHandler : authHandlerList) {
            if (authHandler.support(request)){
                return authHandler;
            }
        }
        return null;
    }



}
