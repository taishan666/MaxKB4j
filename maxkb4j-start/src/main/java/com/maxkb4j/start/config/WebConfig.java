package com.maxkb4j.start.config;

import com.maxkb4j.core.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.*;


@RequiredArgsConstructor
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Async-Executor-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        configurer.setTaskExecutor(executor);
    }

    /**
     * 注册 sa-token 的拦截器
     */
    @Override
    public void addInterceptors(@NotNull InterceptorRegistry registry) {
        // 拦截聊天所有请求
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/chat/api/application/profile")
                .addPathPatterns("/chat/api/open")
                .addPathPatterns("/chat/api/chat_message/*");
    }

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
                        "X-Frame-Options");
    }
}