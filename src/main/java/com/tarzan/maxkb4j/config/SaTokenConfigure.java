/*
package com.tarzan.maxkb4j.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.spring.SpringMVCUtil;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaFoxUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    // 注册拦截器
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，校验规则为 StpUtil.checkLogin() 登录校验。
        registry.addInterceptor(new SaInterceptor(handle -> {
                  if(!StpUtil.isLogin()){
                      SaHolder.getResponse().redirect("http://localhost:3000/login");
                      SaRouter.back();
                  }
                }))
              //  .addPathPatterns("/**")
                .excludePathPatterns("/login");
    }


    */
/** 注册 [Sa-Token全局过滤器] *//*

    @Bean
    public SaServletFilter getSaServletFilter() {
        return new SaServletFilter()
              //  .addInclude("/**")
            //    .addExclude("/sso/*","/oauth2/*" ,"/favicon.ico")
                .setAuth(obj -> {
                    //没登录的时候就去登录
                    if(!StpUtil.isLogin() && SaFoxUtil.isEmpty(SaHolder.getRequest().getParam("satoken"))) {
                        String back = SaFoxUtil.joinParam(SaHolder.getRequest().getUrl(), SpringMVCUtil.getRequest().getQueryString());
                        SaHolder.getResponse().redirect("/sso/login?back=" + SaFoxUtil.encodeUrl(back));
                        SaRouter.back();
                        //登录后但还未授权就去授权
                    }else if (SaFoxUtil.isEmpty(SaHolder.getRequest().getParam("code"))){
                        String back = SaFoxUtil.joinParam(SaHolder.getRequest().getUrl(), SpringMVCUtil.getRequest().getQueryString())+"?satoken="+StpUtil.getTokenValue();
                        SaHolder.getResponse().redirect("http://localhost:9000/oauth2/authorize?response_type=code&client_id=1001&redirect_uri=" + SaFoxUtil.encodeUrl(back)+"&scope=userinfo");
                        SaRouter.back();
                    }
                    //既登录又授权之后,就可以拿到code去请求token了,可以不在这里写
                });
    }
}
*/
