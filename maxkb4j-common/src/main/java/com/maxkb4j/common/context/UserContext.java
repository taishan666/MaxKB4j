package com.maxkb4j.common.context;

import com.maxkb4j.common.interceptor.UserIdentityInterceptor;

/**
 * 当前登录用户的身份访问抽象。
 * <p>业务层(Service / Controller)应依赖此接口获取当前用户,而非直接调用 sa-token 的 {@code StpKit},
 * 从而将"获取当前用户"这一横切关注点从安全框架中解耦,使业务逻辑可脱离 Web 上下文进行单元测试。
 * <p>实现由 {@link UserIdentityInterceptor} 在请求开始时填充、请求结束时清理。
 *
 * @author tarzan
 */
public interface UserContext {

    /**
     * 当前是否已登录(ADMIN 或 USER 任一已解析)。
     */
    boolean isLogin();
    /**
     * 当前用户ID。
     *
     * @throws com.maxkb4j.common.exception.UserIdentityException 当前线程未解析到登录身份时
     */
    String getUserId();
}
