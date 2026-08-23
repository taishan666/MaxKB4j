package com.maxkb4j.start.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 前端 SPA 路由转发过滤器。
 *
 * <p>项目内置两套前端应用（见 src/main/resources/static）：</p>
 * <ul>
 *     <li>{@code /admin}：管理后台，Vue Router 前缀为 /admin，入口 /admin/index.html</li>
 *     <li>{@code /chat}：对话应用，Vue Router 前缀为 /chat，入口 /chat/index.html，含分享页 /chat/share/**</li>
 * </ul>
 *
 * <p>仅当请求命中这两套前端应用的 history 路由（不带扩展名）时转发到对应 index.html；
 * 后端接口、文件下载与静态资源一律放行，交给 Spring MVC / 静态资源处理器。</p>
 *
 * @author tarzan
 */
@Component
@Order(0)
public class SpaForwardFilter extends OncePerRequestFilter {

    /** 管理后台路由前缀 */
    private static final String ADMIN_PREFIX = "/admin";

    /** 对话应用路由前缀 */
    private static final String CHAT_PREFIX = "/chat";

    /** 后端 API 前缀（见 AppConst：ADMIN_API / CHAT_API） */
    private static final String ADMIN_API_PREFIX = "/admin/api/";
    private static final String CHAT_API_PREFIX = "/chat/api/";

    /** 文件下载接口路径（FileController），URL 中不含扩展名，需单独排除 */
    private static final String OSS_FILE_PATH = "/oss/file/";

    /** Knife4j / Swagger 文档地址，统一交给 springdoc 处理 */
    private static final String[] API_DOC_PATHS = {
            "/doc.html", "/webjars/", "/v3/api-docs", "/swagger-ui"
    };

    @Override
    protected boolean shouldNotFilter(@NotNull HttpServletRequest request) {
        String uri = stripContextPath(request);
        return isBackendRequest(uri)
                || isApiDocRequest(uri)
                || isStaticResource(uri);
    }

    /** 后端接口与文件下载直接放行，避免被 SPA 转发吞掉 */
    private boolean isBackendRequest(String uri) {
        return uri.startsWith(ADMIN_API_PREFIX)
                || uri.startsWith(CHAT_API_PREFIX)
                || uri.contains(OSS_FILE_PATH)
                || uri.equals("/error");
    }

    /** Knife4j 文档路径放行 */
    private boolean isApiDocRequest(String uri) {
        for (String path : API_DOC_PATHS) {
            if (uri.startsWith(path)) {
                return true;
            }
        }
        return false;
    }

    /** 最后一个路径段带扩展名视为静态资源（如 /admin/assets/*.js、/favicon.ico） */
    private boolean isStaticResource(String uri) {
        int slashIndex = uri.lastIndexOf('/');
        String lastSegment = slashIndex >= 0 ? uri.substring(slashIndex + 1) : uri;
        return lastSegment.contains(".");
    }

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain chain)
            throws ServletException, IOException {
        String uri = stripContextPath(request);

        // 聊天页的 API 文档入口（前端固定拼接 /chat-api-doc），需先于 /chat 前缀判断
        switch (uri) {
            case "/chat-api-doc" -> {
                request.getRequestDispatcher("/doc.html").forward(request, response);
                return;
            }


            // 根路径重定向到管理后台，避免停留在 / 导致 Vue Router base(/admin) 失效
            case "/" -> {
                response.sendRedirect(request.getContextPath() + ADMIN_PREFIX + "/");
                return;
            }


            // 不带尾斜杠的应用入口重定向，保证路由前缀规范
            case ADMIN_PREFIX -> {
                response.sendRedirect(request.getContextPath() + ADMIN_PREFIX + "/");
                return;
            }
            case CHAT_PREFIX -> {
                response.sendRedirect(request.getContextPath() + CHAT_PREFIX + "/");
                return;
            }
        }

        // 命中的前端路由转发到对应应用的 index.html
        if (uri.startsWith(ADMIN_PREFIX + "/")) {
            request.getRequestDispatcher(ADMIN_PREFIX + "/index.html").forward(request, response);
            return;
        }
        if (uri.startsWith(CHAT_PREFIX + "/")) {
            request.getRequestDispatcher(CHAT_PREFIX + "/index.html").forward(request, response);
            return;
        }

        // 其余未知路径直接放行，由 Spring MVC 处理（404 / 错误页）
        chain.doFilter(request, response);
    }

    /** 去掉 context-path，保证配置了 server.servlet.context-path 时判定依然准确 */
    private String stripContextPath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String uri = request.getRequestURI();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }
}