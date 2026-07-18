package com.maxkb4j.chat.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthHandler {

    boolean handle(HttpServletResponse response);

    boolean support(HttpServletRequest request);
}
