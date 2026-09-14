package com.maxkb4j.start.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
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
 * 后端接口、文件下载与静态资源一律放行，交给 Spring MVC / 静态资源处理器。
 * 另外，index.html 以相对路径（./assets/**）引用构建产物，当 SPA 在深层路由刷新时
 * 浏览器会把相对路径解析到当前路由下（如 /admin/&lt;路由&gt;/assets/x.js），
 * 这类请求会被重定向回规范地址 /admin/assets/x.js，避免静态资源 404 导致白屏。
 * 重定向前会做存在性校验：原始地址本身是真实存在的静态资源（如 /admin/tool/wecom/icon.png）
 * 时直接放行，仅当原始地址不存在而规范地址存在时才还原，避免误伤深层静态资源。</p>
 *
 * @author tarzan
 */
@Component
@Order(0)
public class SpaForwardFilter extends OncePerRequestFilter {

    /**
     * 管理后台路由前缀
     */
    private static final String ADMIN_PREFIX = "/admin";

    /**
     * 对话应用路由前缀
     */
    private static final String CHAT_PREFIX = "/chat";

    /**
     * 后端 API 前缀（见 AppConst：ADMIN_API / CHAT_API）
     */
    private static final String ADMIN_API_PREFIX = "/admin/api/";
    private static final String CHAT_API_PREFIX = "/chat/api/";

    /**
     * 文件下载接口路径（FileController），URL 中不含扩展名，需单独排除
     */
    private static final String OSS_FILE_PATH = "/oss/file/";

    /**
     * Knife4j / Swagger 文档地址，统一交给 springdoc 处理
     */
    private static final String[] API_DOC_PATHS = {
            "/doc.html", "/webjars/", "/v3/api-docs", "/swagger-ui"
    };

    /**
     * Spring Boot 默认静态资源目录（本项目构建产物与图标位于 classpath:/static/）
     */
    private static final String[] STATIC_LOCATIONS = {
            "classpath:/META-INF/resources/", "classpath:/resources/", "classpath:/static/", "classpath:/public/"
    };

    /**
     * 静态资源存在性校验用的资源加载器。
     * 直接实例化而不注入 Spring 容器中的 ResourceLoader Bean（避免与 gridFsTemplate 等
     * 同类型 Bean 产生注入歧义），使用本类类加载器即可正确解析应用 classpath。
     */
    private final ResourceLoader resourceLoader =
            new DefaultResourceLoader(SpaForwardFilter.class.getClassLoader());

    @Override
    protected boolean shouldNotFilter(@NotNull HttpServletRequest request) {
        String uri = stripContextPath(request);
        return isBackendRequest(uri)
                || isApiDocRequest(uri)
                || (isStaticResource(uri) && resolveRouteRelativeAsset(uri) == null);
    }

    /**
     * 后端接口与文件下载直接放行，避免被 SPA 转发吞掉
     */
    private boolean isBackendRequest(String uri) {
        return uri.startsWith(ADMIN_API_PREFIX)
                || uri.startsWith(CHAT_API_PREFIX)
                || uri.contains(OSS_FILE_PATH)
                || uri.equals("/error");
    }

    /**
     * Knife4j 文档路径放行
     */
    private boolean isApiDocRequest(String uri) {
        for (String path : API_DOC_PATHS) {
            if (uri.startsWith(path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 最后一个路径段带扩展名视为静态资源（如 /admin/assets/*.js、/favicon.ico）
     */
    private boolean isStaticResource(String uri) {
        int slashIndex = uri.lastIndexOf('/');
        String lastSegment = slashIndex >= 0 ? uri.substring(slashIndex + 1) : uri;
        return lastSegment.contains(".");
    }

    /**
     * 解析深层路由下错位的静态资源请求。
     *
     * <p>规范地址形如 {@code /admin/assets/x.js}；若 {@code /assets/} 之前还夹着
     * 前端路由段（{@code /admin/application/workspace/{id}/WORK_FLOW/assets/x.js}），
     * 说明是浏览器在深层路由下相对解析出来的错误地址，返回还原后的规范地址。</p>
     *
     * <p>同理，浏览器在深层路由（如 {@code /chat/05938b627de7b0b3/}）下还会按当前
     * 目录去请求应用根级文件（如 {@code favicon.ico}），得到
     * {@code /chat/05938b627de7b0b3/favicon.ico} 这类错位地址，此时还原为
     * {@code /chat/favicon.ico}。仅当末段带扩展名（确为静态文件）时才还原，
     * 避免误伤末段无扩展名的正常 SPA 路由。</p>
     *
     * <p>注意：还原前会校验静态资源存在性。若请求地址本身就是真实存在的静态资源
     * （如 {@code /admin/tool/wecom/icon.png}），直接返回 {@code null} 放行，
     * 交由静态资源处理器响应；仅当请求地址不存在而还原地址存在时才重定向，
     * 避免把真实的深层静态资源误重定向到不存在的根级地址。</p>
     *
     * <p>不属于上述错位请求时返回 {@code null}。</p>
     */
    private String resolveRouteRelativeAsset(String uri) {
        String prefix;
        if (uri.startsWith(ADMIN_PREFIX + "/")) {
            prefix = ADMIN_PREFIX;
        } else if (uri.startsWith(CHAT_PREFIX + "/")) {
            prefix = CHAT_PREFIX;
        } else {
            return null;
        }

        // 构建产物：./assets/** 在深层路由下被解析错位，统一还原到 prefix + /assets/**。
        // 含 /assets/ 的请求全部在此处理：规范地址（/chat/assets/x.css）返回 null 不重定向，
        // 错位地址（/chat/{route}/assets/x.css）才还原，避免落到下方根级文件分支被误剥 assets 段。
        int assetsIndex = uri.indexOf("/assets/");
        if (assetsIndex >= 0) {
            return restoreIfMisplaced(uri, prefix + uri.substring(assetsIndex));
        }

        // 应用根级文件（如 favicon.ico）：深层路由下错位请求，还原到 prefix + /文件名
        String rest = uri.substring(prefix.length());
        int lastSlash = rest.lastIndexOf('/');
        if (lastSlash > 0) {
            String fileName = rest.substring(lastSlash + 1);
            if (fileName.contains(".")) {
                return restoreIfMisplaced(uri, prefix + "/" + fileName);
            }
        }
        return null;
    }

    /**
     * 判断错位静态资源请求是否需要还原：请求地址已是真实存在的静态资源时放行；
     * 仅当请求地址不存在而规范地址存在时才返回规范地址用于重定向
     */
    private String restoreIfMisplaced(String uri, String canonical) {
        if (canonical.equals(uri) || staticResourceExists(uri)) {
            return null;
        }
        return staticResourceExists(canonical) ? canonical : null;
    }

    /**
     * 检查指定 URI 是否为 classpath 下真实存在的静态资源
     */
    private boolean staticResourceExists(String uri) {
        String path = uri.startsWith("/") ? uri.substring(1) : uri;
        for (String location : STATIC_LOCATIONS) {
            try {
                if (resourceLoader.getResource(location + path).exists()) {
                    return true;
                }
            } catch (Exception ignored) {
                // 非法路径等异常按不存在处理
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain chain)
            throws ServletException, IOException {
        String uri = stripContextPath(request);

        // 深层路由下相对引用的静态资源（./assets/**）重定向到规范地址，
        // 让浏览器以正确路径为基准解析资源内部的相对引用（chunk、字体、图片等）
        String assetUri = resolveRouteRelativeAsset(uri);
        if (assetUri != null) {
            response.sendRedirect(request.getContextPath() + assetUri);
            return;
        }

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

    /**
     * 去掉 context-path，保证配置了 server.servlet.context-path 时判定依然准确
     */
    private String stripContextPath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String uri = request.getRequestURI();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }
}