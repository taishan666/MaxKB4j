package com.maxkb4j.common.interceptor;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.jwt.StpLogicJwtForStateless;
import cn.dev33.satoken.jwt.exception.SaJwtException;
import cn.dev33.satoken.stp.StpLogic;
import com.maxkb4j.common.cache.UserTokenRevokeCache;
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
 * 唯一例外是令牌撤销校验:stateless JWT 下 {@code logout} 无法使已签发令牌失效,
 * 禁用/删除用户后需比对 {@link UserTokenRevokeCache} 中的撤销时间戳,使存量令牌立即失效。
 * 非认证异常降级为未登录上下文;token 过期、无效等认证异常继续抛出,由全局异常处理器返回明确提示。
 *
 * <p>注册时 order 需大于 {@code AuthInterceptor}(默认 0),以保证 USER 会话已由 AuthHandler 建立。
 *
 * @author tarzan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemAuthInterceptor implements HandlerInterceptor {

    private final ThreadLocalUserContext userContext;
    private final UserTokenRevokeCache tokenRevokeCache;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        try {
            UserIdentity identity = resolveAdmin();
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



    private UserIdentity resolveAdmin() {
        return resolveIdentity(StpKit.ADMIN, LoginType.ADMIN);
    }

    private UserIdentity resolveIdentity(StpLogic stpLogic, String loginType) {
        try {
            String userId = stpLogic.getLoginIdAsString();
            String tokenValue = stpLogic.getTokenValue();
            if (LoginType.ADMIN.equals(loginType)) {
                // stateless JWT 下禁用/删除用户无法经 logout 失效存量令牌，
                // 按撤销缓存比对令牌签发时间兜底，被撤销令牌抛 TOKEN_FREEZE 异常
                tokenRevokeCache.assertNotRevoked(userId, tokenValue, (StpLogicJwtForStateless) stpLogic);
            }
            return new UserIdentity(userId, tokenValue, loginType);
        } catch (NotLoginException e) {
            if (NotLoginException.NOT_TOKEN.equals(e.getType())) {
                return null;
            }else if (NotLoginException.INVALID_TOKEN.equals(e.getType())) {
                return null;
            } else if (NotLoginException.TOKEN_TIMEOUT.equals(e.getType())) {
                return null;
            }
            throw e;
        }
    }

}
