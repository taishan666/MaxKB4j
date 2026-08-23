package com.maxkb4j.start.config;

import com.maxkb4j.common.interceptor.UserIdentityInterceptor;
import com.maxkb4j.common.web.CurrentUserIdArgumentResolver;
import com.maxkb4j.chat.interceptor.AuthInterceptor;
import com.maxkb4j.system.interceptor.LocaleInterceptor;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.*;

import java.util.List;


@RequiredArgsConstructor
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final LocaleInterceptor localeInterceptor;
    private final UserIdentityInterceptor userIdentityInterceptor;
    private final CurrentUserIdArgumentResolver currentUserIdArgumentResolver;

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("async-");
        executor.setVirtualThreads(true);
        configurer.setTaskExecutor(executor);
    }


    /**
     * 注册 sa-token 的拦截器
     */
    @Override
    public void addInterceptors(@NotNull InterceptorRegistry registry) {
        // 国际化语言解析（最高优先级，覆盖所有请求）
        registry.addInterceptor(localeInterceptor)
                .addPathPatterns("/**")
                .order(Integer.MIN_VALUE);
        // 拦截聊天所有请求
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/chat/api/application/profile")
                .addPathPatterns("/chat/api/open")
                .addPathPatterns("/chat/api/chat_message/{chatId}")
                .addPathPatterns("/chat/api/historical_conversation/{current}/{size}")
                .addPathPatterns("/chat/api/historical_conversation/clear");
        // 用户身份解析（填充 UserContext，供业务层获取当前用户；order=1 保证晚于 authInterceptor 解析 USER 会话）
        registry.addInterceptor(userIdentityInterceptor)
                .addPathPatterns("/**")
                .order(1);
    }

    @Override
    public void addArgumentResolvers(@NotNull List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserIdArgumentResolver);
    }

    @Override
    public void addResourceHandlers(@NotNull ResourceHandlerRegistry registry) {
/*        // 映射 doc.html
        registry.addResourceHandler("/doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        // 映射 webjars 静态资源
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");*/
    }
/*
    @Override
    public void addViewControllers(@NotNull ViewControllerRegistry registry) {
        registry.addViewController("/admin/{path:[^.]*}").setViewName("forward:/admin/index.html");
        registry.addViewController("/admin/{path1:[^.]*}/{path2:[^.]*}").setViewName("forward:/admin/index.html");
        registry.addViewController("/admin/{path1:[^.]*}/{path2:[^.]*}/{path3:[^.]*}").setViewName("forward:/admin/index.html");
        registry.addViewController("/admin/{path1:[^.]*}/{path2:[^.]*}/{path3:[^.]*}/{path4:[^.]*}").setViewName("forward:/admin/index.html");
        registry.addViewController("/admin/{path1:[^.]*}/{path2:[^.]*}/{path3:[^.]*}/{path4:[^.]*}/{path5:[^.]*}").setViewName("forward:/admin/index.html");
        registry.addViewController("/chat/{path:[^.]*}").setViewName("forward:/chat/index.html");
        registry.addViewController("/chat/share/{path:[^.]*}").setViewName("forward:/chat/index.html");
        registry.addViewController("/chat-api-doc").setViewName("forward:/doc.html");
    }*/

    /**
     * 注意：前端路由转发由 SpaForwardFilter 处理，不使用 ViewControllerRegistry
     * 因为 ViewControllerRegistry 的优先级高于@RestController，会导致 API 请求被错误转发
     */

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 添加映射路径
        registry.addMapping("/**")
                // 放行哪些原始域
                .allowedOriginPatterns("*")
                // 是否发送 Cookie 信息
                .allowCredentials(true)
                // 放行哪些原始域 (请求方式)
                .allowedMethods("*")
                // 放行哪些原始域 (头部信息)
                .allowedHeaders("*")
                // 暴露哪些头部信息（因为跨域访问默认不能获取全部头部信息）
                .exposedHeaders("access-control-allow-headers",
                        "access-control-allow-methods",
                        "access-control-allow-origin",
                        "access-control-max-age",
                        "X-Frame-Options").maxAge(300);
    }
}