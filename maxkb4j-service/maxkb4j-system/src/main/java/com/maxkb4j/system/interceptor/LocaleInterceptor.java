package com.maxkb4j.system.interceptor;

import com.maxkb4j.common.util.StpKit;
import com.maxkb4j.user.service.IUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Locale;

/**
 * 语言解析拦截器
 * <p>
 * 解析顺序：
 * <ol>
 *   <li>已登录用户的 user.language 字段</li>
 *   <li>默认 zh_CN</li>
 * </ol>
 * 解析后通过 {@link LocaleContextHolder#setLocale(Locale)} 写入当前线程，
 * 供 {@link com.maxkb4j.common.util.I18nUtil} 与 Spring MessageSource 使用。
 *
 * @author tarzan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocaleInterceptor implements HandlerInterceptor {

    private static final Locale DEFAULT_LOCALE = Locale.SIMPLIFIED_CHINESE;

    private final IUserService userService;

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request,
                             @NotNull HttpServletResponse response,
                             @NotNull Object handler) {
        Locale locale = resolveLocale(request);
        LocaleContextHolder.setLocale(locale);
        return true;
    }

    @Override
    public void afterCompletion(@NotNull HttpServletRequest request,
                                @NotNull HttpServletResponse response,
                                @NotNull Object handler,
                                Exception ex) {
        // 清理 ThreadLocal，避免线程复用造成的串扰
        LocaleContextHolder.resetLocaleContext();
    }

    private Locale resolveLocale(HttpServletRequest request) {
        // 1) 已登录用户的语言偏好（从 user 表 language 字段获取）
        try {
            if (StpKit.ADMIN.isLogin()) {
                String userId = StpKit.ADMIN.getLoginIdAsString();
                Locale locale = parseLocale(userService.getLanguage(userId));
                return locale == null ? DEFAULT_LOCALE : locale;
            }
        } catch (Exception e) {
            log.debug("resolve user locale failed: {}", e.getMessage());
        }

        // 2) 默认
        return DEFAULT_LOCALE;
    }

    /**
     * 将 "zh-CN" / "zh_CN" / "en-US" / "en" 之类的字符串解析为受支持的 Locale。
     * 仅支持简体中文与英文两种语言；不在白名单内一律返回 null（让上层降级到下一来源）。
     */
    private Locale parseLocale(String tag) {
        if (tag == null || tag.isBlank()) {
            return null;
        }
        String normalized = tag.replace('_', '-').toLowerCase();
        if (normalized.startsWith("zh")) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        if (normalized.startsWith("en")) {
            return Locale.US;
        }
        return null;
    }
}
