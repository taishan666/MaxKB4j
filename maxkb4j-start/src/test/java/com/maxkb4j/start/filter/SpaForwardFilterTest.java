package com.maxkb4j.start.filter;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockRequestDispatcher;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * SpaForwardFilter 单元测试：SPA 路由转发与深层路由下相对资源引用的重定向
 *
 * @author tarzan
 */
class SpaForwardFilterTest {

    private SpaForwardFilter filter;

    @BeforeEach
    void setUp() {
        filter = new SpaForwardFilter();
    }

    /** 执行过滤器并捕获转发目标（forward 的路径无法直接断言，需捕获 getRequestDispatcher 入参） */
    private static class Capture {
        final MockHttpServletRequest request;
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain chain = new MockFilterChain();
        final AtomicReference<String> forwarded = new AtomicReference<>();

        Capture(String method, String uri) {
            request = new MockHttpServletRequest(method, uri) {
                @Override
                public RequestDispatcher getRequestDispatcher(String path) {
                    forwarded.set(path);
                    return new MockRequestDispatcher(path);
                }
            };
        }

        void doFilter(SpaForwardFilter filter) throws Exception {
            filter.doFilter(request, response, chain);
        }
    }

    @Test
    void 深层前端路由转发到admin入口() throws Exception {
        Capture c = new Capture("GET", "/admin/application/workspace/a697f3b7559ee5211c3ec1cb4feb4185/WORK_FLOW");
        c.doFilter(filter);
        assertEquals("/admin/index.html", c.forwarded.get());
        assertNull(c.response.getRedirectedUrl());
    }

    @Test
    void 深层路由下相对引用的资源重定向到规范地址() throws Exception {
        Capture c = new Capture("GET",
                "/admin/application/workspace/a697f3b7559ee5211c3ec1cb4feb4185/WORK_FLOW/assets/admin-CQ3GoTdO.js");
        c.doFilter(filter);
        assertEquals("/admin/assets/admin-CQ3GoTdO.js", c.response.getRedirectedUrl());
        assertNull(c.forwarded.get());
    }

    @Test
    void 深层路由下相对引用的样式重定向到规范地址() throws Exception {
        Capture c = new Capture("GET",
                "/admin/application/workspace/a697f3b7559ee5211c3ec1cb4feb4185/WORK_FLOW/assets/admin-CL-BdUAo.css");
        c.doFilter(filter);
        assertEquals("/admin/assets/admin-CL-BdUAo.css", c.response.getRedirectedUrl());
    }

    @Test
    void chat分享页深层路由下相对引用的资源重定向到规范地址() throws Exception {
        Capture c = new Capture("GET", "/chat/share/abc123/assets/chat-BNGetJfI.js");
        c.doFilter(filter);
        assertEquals("/chat/assets/chat-BNGetJfI.js", c.response.getRedirectedUrl());
    }

    @Test
    void chat分享路由转发到chat入口() throws Exception {
        Capture c = new Capture("GET", "/chat/share/abc123");
        c.doFilter(filter);
        assertEquals("/chat/index.html", c.forwarded.get());
    }

    @Test
    void 规范的静态资源地址直接放行() throws Exception {
        Capture c = new Capture("GET", "/admin/assets/admin-CQ3GoTdO.js");
        c.doFilter(filter);
        assertNull(c.forwarded.get());
        assertNull(c.response.getRedirectedUrl());
        assertNotNull(c.chain.getRequest(), "应由静态资源处理器接手，过滤器直接放行");
    }

    @Test
    void 后端接口即使带扩展名也直接放行() throws Exception {
        Capture c = new Capture("GET", "/admin/api/application/export.docx");
        c.doFilter(filter);
        assertNull(c.forwarded.get());
        assertNull(c.response.getRedirectedUrl());
        assertNotNull(c.chain.getRequest());
    }

    @Test
    void 根路径重定向到管理后台() throws Exception {
        Capture c = new Capture("GET", "/");
        c.doFilter(filter);
        assertEquals("/admin/", c.response.getRedirectedUrl());
    }
}
