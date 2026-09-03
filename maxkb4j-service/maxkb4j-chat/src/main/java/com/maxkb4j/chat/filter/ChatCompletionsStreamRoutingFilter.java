package com.maxkb4j.chat.filter;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.constant.AppConst;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
/**
 * Routes the OpenAI-compatible chat completions endpoint to the SSE or the JSON
 * handler based on the {@code stream} field of the request body, regardless of the
 * client-side {@code Accept} header.
 *
 * <p>The controller exposes two mappings with the same path, distinguished by
 * {@code produces}. This filter parses the body once, caches it for the controller,
 * and rewrites the {@code Accept} header according to the {@code stream} flag so that
 * content negotiation follows the request payload instead of the client-declared
 * Accept value.</p>
 */
@Component
@Order(1)
public class ChatCompletionsStreamRoutingFilter extends OncePerRequestFilter {
    private static final String ACCEPT_HEADER = "Accept";
    private static final String CHAT_COMPLETIONS_SUFFIX = "/chat/completions";
    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain)
            throws ServletException, IOException {
        if (!isChatCompletionsRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        byte[] body = request.getInputStream().readAllBytes();
        String accept = isStreamingRequest(body)
                ? MediaType.TEXT_EVENT_STREAM_VALUE
                : MediaType.APPLICATION_JSON_VALUE;
        filterChain.doFilter(new AcceptHeaderRequest(new CachedBodyRequest(request, body), accept), response);
    }
    private boolean isChatCompletionsRequest(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String uri = stripContextPath(request);
        return uri.startsWith("/" + AppConst.CHAT_API + "/") && uri.endsWith(CHAT_COMPLETIONS_SUFFIX);
    }
    private boolean isStreamingRequest(byte[] body) {
        if (body == null || body.length == 0) {
            return false;
        }
        try {
            JSONObject json = JSONObject.parseObject(new String(body, StandardCharsets.UTF_8));
            return json != null && Boolean.TRUE.equals(json.getBoolean("stream"));
        } catch (Exception e) {
            return false;
        }
    }
    private String stripContextPath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String uri = request.getRequestURI();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }
    private static class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;
        CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }
        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream in = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return in.available() == 0;
                }
                @Override
                public boolean isReady() {
                    return true;
                }
                @Override
                public void setReadListener(ReadListener readListener) {
                    throw new UnsupportedOperationException("streaming body read is not supported");
                }
                @Override
                public int read() {
                    return in.read();
                }
            };
        }
        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
    private static class AcceptHeaderRequest extends HttpServletRequestWrapper {
        private final String accept;
        AcceptHeaderRequest(HttpServletRequest request, String accept) {
            super(request);
            this.accept = accept;
        }
        @Override
        public String getHeader(String name) {
            return ACCEPT_HEADER.equalsIgnoreCase(name) ? accept : super.getHeader(name);
        }
        @Override
        public Enumeration<String> getHeaders(String name) {
            return ACCEPT_HEADER.equalsIgnoreCase(name)
                    ? Collections.enumeration(List.of(accept))
                    : super.getHeaders(name);
        }
    }
}
