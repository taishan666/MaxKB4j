package com.maxkb4j.common.interceptor;

import com.maxkb4j.common.constant.LoginType;
import com.maxkb4j.common.context.ThreadLocalUserContext;
import com.maxkb4j.common.context.UserIdentity;
import com.maxkb4j.common.util.StpKit;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 请求身份解析拦截器。
 * <p>在请求开始时从 sa-token 解析当前登录身份(ADMIN 或 USER),写入 {@link ThreadLocalUserContext},
 * 使业务层可通过 {@link com.maxkb4j.common.context.UserContext} 获取当前用户,而无需直接依赖 sa-token。
 *
 * <p>解析策略(按请求路径决定优先级):
 * <ol>
 *   <li>chat 路径({@code /chat/**}):优先解析 {@link StpKit#USER},避免同浏览器同时登录 ADMIN+USER 时误判为 ADMIN。</li>
 *   <li>其他路径:优先解析 {@link StpKit#ADMIN},ADMIN 未命中时再尝试 USER。</li>
 * </ol>
 *
 * <p>本拦截器只负责"填充上下文",不承担鉴权(鉴权由 {@code AuthInterceptor}、{@code SaCheckPermAspect} 等负责),
 * 因此任何分支均返回 {@code true},且解析过程对外抛出被吞掉(仅 debug 日志),绝不阻断请求。
 *
 * <p>注册时 order 需大于 {@code AuthInterceptor}(默认 0),以保证 USER 会话已由 AuthHandler 建立。
 *
 * @author tarzan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserIdentityInterceptor implements HandlerInterceptor {

    private final ThreadLocalUserContext userContext;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        try {
            UserIdentity identity = resolve(request);
            if (identity != null) {
                userContext.set(identity);
            }
        } catch (Exception e) {
            // 身份解析失败不应影响请求处理,降级为"未登录"上下文
            log.debug("解析用户身份失败,降级为未登录: {}", e.getMessage());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        // 清理 ThreadLocal,避免线程池复用造成的身份串扰
        userContext.clear();
    }

    private static final String CHAT_PATH_PREFIX = "/chat";

    private UserIdentity resolve(HttpServletRequest request) {
        boolean isChatPath = request.getRequestURI().startsWith(CHAT_PATH_PREFIX);
        if (isChatPath) {
            // chat 路径:优先解析 USER,避免同浏览器双登录时误取 ADMIN 身份
            return  resolveUser();
        }
        // 其他路径:优先解析 ADMIN
        return resolveAdmin();
    }

    private UserIdentity resolveAdmin() {
        if (StpKit.ADMIN.isLogin()) {
            return new UserIdentity(StpKit.ADMIN.getLoginIdAsString(), StpKit.ADMIN.getTokenValue(), LoginType.ADMIN);
        }
        return null;
    }

    private UserIdentity resolveUser() {
        if (StpKit.USER.isLogin()) {
            return new UserIdentity(StpKit.USER.getLoginIdAsString(), StpKit.USER.getTokenValue(), LoginType.USER);
        }
        return null;
    }

}
