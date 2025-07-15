
package com.tarzan.maxkb4j.config;

import com.tarzan.maxkb4j.core.interceptor.AuthInterceptor;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.*;


@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(asyncExecutor());
    }

    @Bean(name = "asyncExecutor")
    public AsyncTaskExecutor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Async-Executor-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }


    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
       // registry.addResourceHandler("/favicon.ico").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
        //放行所有的静态资源，为了保证vue项目可以正常使用
        //registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
    }

    /**
     * 注册sa-token的拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 拦截聊天所有请求
        registry.addInterceptor(new AuthInterceptor()).addPathPatterns("/api/application/chat/**");
    }


    @Override
    public void addViewControllers(@NotNull ViewControllerRegistry registry) {
        System.out.println(registry);
        registry.addViewController("/ui/{path:[^.]*}").setViewName("forward:/ui/index.html");
        registry.addViewController("/ui/{path:[^.]*}/{path1:[^.]*}").setViewName("forward:/ui/index.html");
        registry.addViewController("/ui/{path:[^.]*}/{path1:[^.]*}/{path2:[^.]*}").setViewName("forward:/ui/index.html");
        registry.addViewController("/ui/{path:[^.]*}/{path1:[^.]*}/{path2:[^.]*}/{path3:[^.]*}").setViewName("forward:/ui/index.html");
        registry.addViewController("/ui/{path:[^.]*}/{path1:[^.]*}/{path2:[^.]*}/{path3:[^.]*}/{path4:[^.]*}").setViewName("forward:/ui/index.html");
    }


   /*  *//**
     * 解决全局跨域，
     *
      *//*
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
    }*/
}
