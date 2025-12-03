package com.tarzan.maxkb4j.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.regex.Pattern;

@Component
public class StaticResourceFilter implements Filter {

    private static final String REGEX = "^/admin/.*/assets/[\\w.-]+$";
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    public static boolean matches(String path) {
        return PATTERN.matcher(path).matches();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();
        if (path.startsWith("/admin/") && path.endsWith("/favicon.ico")) {
            RequestDispatcher dispatcher = request.getRequestDispatcher("/admin/favicon.ico");
            dispatcher.forward(request, response);
        } else if (matches(path)) {
            String assetPath = path.substring(path.lastIndexOf("/"));
            RequestDispatcher dispatcher = request.getRequestDispatcher("/admin/assets"+assetPath);
            dispatcher.forward(request, response);
        } else {
            chain.doFilter(request, response);
        }
    }
}