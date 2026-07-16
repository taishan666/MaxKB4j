package com.maxkb4j.common.context;

import com.maxkb4j.common.interceptor.UserIdentityInterceptor;

import java.util.Map;

/**
 * 当前请求解析出的用户身份快照。
 * <p>由 {@link UserIdentityInterceptor} 从 sa-token 会话解析后写入 {@link ThreadLocalUserContext},
 * 随后的业务层调用只能读到这份快照,而不再触碰 sa-token 的静态 API。
 *
 * @author tarzan
 */
public record UserIdentity(String userId, String loginType, Map<String, Object> extras) {

    public UserIdentity(String userId, String loginType, Map<String, Object> extras) {
        this.userId = userId;
        this.loginType = loginType;
        this.extras = extras == null ? Map.of() : extras;
    }

}
