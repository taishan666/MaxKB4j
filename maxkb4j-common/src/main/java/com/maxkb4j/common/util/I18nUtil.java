package com.maxkb4j.common.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 国际化工具类
 * <p>
 * 通过当前线程上下文（{@link LocaleContextHolder}）解析的 Locale，
 * 对 i18n/messages*.properties 中配置的键进行翻译。
 * <p>
 * 用法：{@code I18nUtil.get("login.captcha.error")}
 *
 * @author tarzan
 */
@Component
public class I18nUtil implements ApplicationContextAware {

    private static MessageSource MESSAGE_SOURCE;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        MESSAGE_SOURCE = applicationContext.getBean(MessageSource.class);
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
        if (MESSAGE_SOURCE == null) {
            return code;
        }
        try {
            return MESSAGE_SOURCE.getMessage(code, args, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException ex) {
            return code;
        }
    }

    /**
     * 指定 Locale 翻译
     */
    public static String get(String code, Locale locale, Object... args) {
        if (MESSAGE_SOURCE == null) {
            return code;
        }
        try {
            return MESSAGE_SOURCE.getMessage(code, args, locale);
        } catch (NoSuchMessageException ex) {
            return code;
        }
    }
}
