package com.maxkb4j.common.context;

import com.maxkb4j.common.exception.UserIdentityException;
import com.maxkb4j.common.interceptor.UserIdentityInterceptor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 基于 {@link ThreadLocal} 的 {@link UserContext} 实现。
 * <p>由 {@link UserIdentityInterceptor} 在请求开始时填充、请求结束时清理,
 * 与 Spring {@code LocaleContextHolder} 同样的线程绑定模型。业务层通过依赖注入获取此单例实例,
 * 读方法只访问 {@link ThreadLocal} 快照,不依赖 sa-token,因此可独立单元测试。
 *
 * @author tarzan
 */
@Component
public class ThreadLocalUserContext implements UserContext {

    private static final ThreadLocal<UserIdentity> HOLDER = new ThreadLocal<>();

    /**
     * 写入当前线程身份快照(仅限解析层调用)。
     */
    public void set(UserIdentity identity) {
        HOLDER.set(identity);
    }

    /**
     * 清理当前线程身份(仅限解析层调用,避免线程池复用造成的串扰)。
     */
    public void clear() {
        HOLDER.remove();
    }

    @Override
    public boolean isLogin() {
        return HOLDER.get() != null;
    }

    @Override
    public String getUserId() {
        UserIdentity identity = HOLDER.get();
        if (identity == null) {
            throw new UserIdentityException("当前线程未解析到登录身份");
        }
        return identity.userId();
    }
}
