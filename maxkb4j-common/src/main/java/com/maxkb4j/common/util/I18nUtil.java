package com.maxkb4j.common.util;

import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 国际化工具类
 * <p>
 * 通过当前线程上下文（{@link LocaleContextHolder}）解析的 Locale，
 * 对 i18n/messages*.properties 中配置的键进行翻译。
 * <p>
 * 用法：{@code I18nUtil.get("login.captcha.error")}
 * <p>
 * 设计说明：{@link MessageSource} 通过构造器注入为普通 Bean（不再使用
 * {@code ApplicationContextAware} 手动取 Bean）。由于 {@code R}、{@code ApiException}
 * 等非 Spring 管理的类仍需静态调用，这里保留静态门面方法，并通过
 * {@link #setMessageSource(MessageSource)} 暴露测试替换入口。
 *
 * @author tarzan
 */
@Component
public class I18nUtil {

    private static volatile MessageSource messageSource;

    /**
     * Spring 构造器注入；将实例注册到静态持有器供静态门面方法使用。
     */
    public I18nUtil(MessageSource messageSource) {
        I18nUtil.messageSource = messageSource;
    }

    /**
     * 按当前 Locale 翻译 key；若无对应消息则返回 key 本身（避免抛错）
     */
    public static String get(String code) {
        return get(code, (Object[]) null);
    }

    /**
     * 按当前 Locale 翻译 key，并使用 args 替换占位符
     */
    public static String get(String code, Object... args) {
        return get(code, LocaleContextHolder.getLocale(), args);
    }

    /**
     * 指定 Locale 翻译
     */
    public static String get(String code, Locale locale, Object... args) {
        MessageSource source = messageSource;
        if (source == null) {
            return code;
        }
        try {
            return source.getMessage(code, args, locale);
        } catch (NoSuchMessageException ex) {
            return code;
        }
    }

    /**
     * 供测试或非 Spring 上下文注入/替换 {@link MessageSource}（例如 Mock）。
     */
    public static void setMessageSource(@Nullable MessageSource source) {
        messageSource = source;
    }
}
