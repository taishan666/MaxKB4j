package com.maxkb4j.common.interceptor;

import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.constant.LoginType;
import com.maxkb4j.common.context.ThreadLocalUserContext;
import com.maxkb4j.common.context.UserIdentity;
import com.maxkb4j.common.util.StpKit;
import com.maxkb4j.common.util.WebUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 请求身份解析拦截器。
 * <p>在请求开始时从 sa-token 解析当前登录身份(ADMIN 或 USER),写入 {@link ThreadLocalUserContext},
 * 使业务层可通过 {@link com.maxkb4j.common.context.UserContext} 获取当前用户,而无需直接依赖 sa-token。
 *
 * <p>解析顺序:
 * <ol>
 *   <li>{@link StpKit#ADMIN}:ADMIN 会话由 sa-token 按其 token 名自动解析(无需手动桥接);命中则快照身份。</li>
 *   <li>{@link StpKit#USER}:仅对 chat 路径,将 {@code Authorization} 头中的 token 桥接进 USER 会话后再解析,
 *       集中替代原本散落在 {@code ChatApiService}/{@code ChatApiController} 中的 {@code setTokenValue} 调用。</li>
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

    /**
     * chat 路径登录时写入的扩展属性键集合(应用与 chat 端点的 JWT extra 契约)。
     * 解析时一次性快照,避免业务层再触碰 sa-token。
     */
    private static final List<String> USER_EXTRA_KEYS = List.of("applicationId", "chatUserType", "accessToken");

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

    private UserIdentity resolve(HttpServletRequest request) {
        // 1) ADMIN:sa-token 按 token 名自动解析
        if (StpKit.ADMIN.isLogin()) {
            return new UserIdentity(StpKit.ADMIN.getLoginIdAsString(), LoginType.ADMIN, Map.of());
        }
        // 2) USER:仅对 chat 路径桥接 Authorization 头中的 token 后解析
        if (!isChatRequest(request)) {
            return null;
        }
        String token = WebUtil.getTokenValue();
        if (token == null) {
            return null;
        }
        StpKit.USER.setTokenValue(token);
        if (!StpKit.USER.isLogin()) {
            return null;
        }
        return new UserIdentity(
                StpKit.USER.getLoginIdAsString(),
                LoginType.USER,
                snapshotUserExtras()
        );
    }

    private boolean isChatRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/" + AppConst.CHAT_API);
    }

    private Map<String, Object> snapshotUserExtras() {
        Map<String, Object> extras = new LinkedHashMap<>(USER_EXTRA_KEYS.size());
        for (String key : USER_EXTRA_KEYS) {
            Object value = StpKit.USER.getExtra(key);
            if (value != null) {
                extras.put(key, value);
            }
        }
        return extras;
    }
}
