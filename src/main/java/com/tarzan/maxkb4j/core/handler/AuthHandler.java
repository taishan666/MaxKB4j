package com.tarzan.maxkb4j.core.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthHandler {

    boolean handle(HttpServletResponse response);

    boolean support(HttpServletRequest request);
}
