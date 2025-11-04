package com.tarzan.maxkb4j.config;

import cn.dev33.satoken.jwt.StpLogicJwtForStateless;
import cn.dev33.satoken.stp.StpLogic;
import com.tarzan.maxkb4j.module.application.enums.ChatUserType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class SaTokenConfigure {
    // Sa-Token 整合 jwt (Stateless 无状态模式)
    @Primary
    @Bean
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForStateless();
    }

    // 普通用户
    @Bean
    public StpLogic stpLogicUser() {
        return new StpLogicJwtForStateless(ChatUserType.CHAT_USER.name());
    }

    // 匿名用户
    @Bean
    public StpLogic stpLogicAnonymousUser() {
        return new StpLogicJwtForStateless(ChatUserType.ANONYMOUS_USER.name());
    }

}

