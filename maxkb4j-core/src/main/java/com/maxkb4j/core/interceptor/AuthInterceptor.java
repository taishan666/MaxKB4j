package com.maxkb4j.core.interceptor;

import com.maxkb4j.core.handler.AuthHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collection;


@Component
@Slf4j
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final Collection<AuthHandler> authHandlerList;

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
