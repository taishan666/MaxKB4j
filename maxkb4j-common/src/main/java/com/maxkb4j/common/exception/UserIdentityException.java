package com.maxkb4j.common.exception;

/**
 * 业务层在缺少登录身份上下文时访问用户身份抛出。
 * <p>例如:在未登录或非请求线程中调用 {@code UserContext.getUserId()}。
 * 用于将"未登录"这一安全状态以业务可识别的方式向调用方传递,替代直接依赖 sa-token 的 {@code NotLoginException}。
 *
 * @author tarzan
 */
public class UserIdentityException extends RuntimeException {

    public UserIdentityException() {
        super();
    }

    public UserIdentityException(String message) {
        super(message);
    }
}
