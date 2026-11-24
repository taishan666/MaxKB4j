package com.maxkb4j.start.config;

import cn.dev33.satoken.jwt.StpLogicJwtForStateless;
import cn.dev33.satoken.stp.StpLogic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SaTokenConfigure {
    // Sa-Token 整合 jwt (Stateless 无状态模式)
    // 注意:无状态模式令牌自包含、服务端不存会话,logout/kickout 等操作无法使已签发令牌失效;
    // 禁用用户、删除用户、重置口令等场景由 UserTokenRevokeCache 按令牌签发时间(eff)比对兜底失效,
    // 校验入口位于 SystemAuthInterceptor
    //
    // 不标注 @Primary，因为 Spring 容器中只应有一个 StpLogic 默认实例。
    // 多 loginType 的 StpLogic 实例（ADMIN/USER）已由 StpKit 以静态字段管理，不注册为 Spring Bean。
    // 保留 @Primary 会潜在遮蔽后续新增 StpLogic Bean 时的类型冲突，违反 fail-fast 原则。
    @Bean
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForStateless();
    }

}

