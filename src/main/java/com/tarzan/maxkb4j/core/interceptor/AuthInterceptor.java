package com.tarzan.maxkb4j.core.interceptor;

import com.tarzan.maxkb4j.common.util.SpringUtil;
import com.tarzan.maxkb4j.module.application.handler.AuthHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collection;


@Slf4j
public class AuthInterceptor implements HandlerInterceptor {

    private static final Collection<AuthHandler> authHandlerList = SpringUtil.getBeansOfType(AuthHandler.class).values();

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) {
        AuthHandler authHandler = getAuthHandler(request);
        if (authHandler!=null){
            return authHandler.handle(response);
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
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
