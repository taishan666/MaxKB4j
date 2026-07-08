package com.maxkb4j.common.context;

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
     * 当前登录类型,对应 {@link com.maxkb4j.common.constant.LoginType};未登录时返回 {@code null}。
     */
    String getLoginType();

    /**
     * 当前用户ID。
     *
     * @throws com.maxkb4j.common.exception.UserIdentityException 当前线程未解析到登录身份时
     */
    String getUserId();

    /**
     * 读取登录时存入的扩展属性(JWT extra claim),如 applicationId / chatUserType / accessToken。
     * 未登录或属性不存在时返回 {@code null}。
     */
    String getExtra(String key);
}
