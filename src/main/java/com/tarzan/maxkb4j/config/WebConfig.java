
package com.tarzan.maxkb4j.config;

import com.tarzan.maxkb4j.core.interceptor.AuthInterceptor;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.*;


@Configuration
public class WebConfig implements WebMvcConfigurer {

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
     * 注册sa-token的拦截器
     */
    @Override
    public void addInterceptors(@NotNull InterceptorRegistry registry) {
        // 拦截聊天所有请求
        registry.addInterceptor(new AuthInterceptor())
                .addPathPatterns("/chat/api/application/profile")
                .addPathPatterns("/chat/api/open")
                .addPathPatterns("/chat/api/chat_message/*");
    }



    @Override
    public void addResourceHandlers(@NotNull ResourceHandlerRegistry registry) {
    }


    @Override
    public void addViewControllers(@NotNull ViewControllerRegistry registry) {
        registry.addViewController("/admin/{path:[^.]*}").setViewName("forward:/admin/index.html");
        registry.addViewController("/admin/{path1:[^.]*}/{path2:[^.]*}").setViewName("forward:/admin/index.html");
        registry.addViewController("/admin/{path1:[^.]*}/{path2:[^.]*}/{path3:[^.]*}").setViewName("forward:/admin/index.html");
        registry.addViewController("/admin/{path1:[^.]*}/{path2:[^.]*}/{path3:[^.]*}/{path4:[^.]*}").setViewName("forward:/admin/index.html");
        registry.addViewController("/admin/{path1:[^.]*}/{path2:[^.]*}/{path3:[^.]*}/{path4:[^.]*}/{path5:[^.]*}").setViewName("forward:/admin/index.html");
        registry.addViewController("/chat/{path:[^.]*}").setViewName("forward:/chat/index.html");
        registry.addViewController("/admin/application/workspace/{path:[^.]*}/favicon.ico").setViewName("forward:/favicon.ico");
        registry.addViewController("/admin/application/workspace/{path:[^.]*}/{path1:[^.]*}/favicon.ico").setViewName("forward:/favicon.ico");
        registry.addViewController("/admin/knowledge/{path:[^.]*}/{path1:[^.]*}/favicon.ico").setViewName("forward:/favicon.ico");
        registry.addViewController("/admin/knowledge/{path:[^.]*}/{path1:[^.]*}/{path2:[^.]*}/favicon.ico").setViewName("forward:/favicon.ico");
        registry.addViewController("/admin/system/{path:[^.]*}/favicon.ico").setViewName("forward:/favicon.ico");
        registry.addViewController("/chat-api-doc").setViewName("forward:/doc.html");
    }



    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 添加映射路径
        registry.addMapping("/**")
                // 放行哪些原始域
                .allowedOriginPatterns("*")
                // 是否发送Cookie信息
                .allowCredentials(true)
                // 放行哪些原始域(请求方式)
                .allowedMethods("*")
                // 放行哪些原始域(头部信息)
                .allowedHeaders("*")
                // 暴露哪些头部信息（因为跨域访问默认不能获取全部头部信息）
                .exposedHeaders("access-control-allow-headers",
                        "access-control-allow-methods",
                        "access-control-allow-origin",
                        "access-control-max-age",
                        "X-Frame-Options");
    }
}
