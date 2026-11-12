package com.maxkb4j.common.interceptor;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.jwt.exception.SaJwtException;
import cn.dev33.satoken.stp.StpLogic;
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
 * <p>本拦截器只负责"填充上下文",不承担鉴权(鉴权由 {@code AuthInterceptor}、{@code SaCheckPermAspect} 等负责)。
 * 非认证异常降级为未登录上下文;token 过期、无效等认证异常继续抛出,由全局异常处理器返回明确提示。
 *
 * <p>注册时 order 需大于 {@code AuthInterceptor}(默认 0),以保证 USER 会话已由 AuthHandler 建立。
 *
 * @author tarzan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserIdentityInterceptor implements HandlerInterceptor {

    private static final String CHAT_PATH_PREFIX = "/chat";
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
        } catch (NotLoginException | SaJwtException e) {
            throw e;
        } catch (Exception e) {
            // 非认证异常不应影响请求处理,降级为"未登录"上下文
            log.error("解析用户身份失败,降级为未登录: {}", e.getMessage());
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
        boolean isChatPath = request.getRequestURI().startsWith(CHAT_PATH_PREFIX);
        if (isChatPath) {
            // chat 路径:优先解析 USER,避免同浏览器双登录时误取 ADMIN 身份
            return resolveUser();
        }
        // 其他路径:优先解析 ADMIN,未命中再回退 USER
        UserIdentity identity = resolveAdmin();
        return identity != null ? identity : resolveUser();
    }

    private UserIdentity resolveAdmin() {
        return resolveIdentity(StpKit.ADMIN, LoginType.ADMIN);
    }

    private UserIdentity resolveUser() {
        return resolveIdentity(StpKit.USER, LoginType.USER);
    }

    private UserIdentity resolveIdentity(StpLogic stpLogic, String loginType) {
        try {
            return new UserIdentity(stpLogic.getLoginIdAsString(), stpLogic.getTokenValue(), loginType);
        } catch (NotLoginException e) {
            if (NotLoginException.NOT_TOKEN.equals(e.getType())) {
                return null;
            }
            throw e;
        }
    }

}
