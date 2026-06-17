package com.maxkb4j.start.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 国际化配置
 * <p>
 * MessageSource 支持 .properties 与 .yml 两种消息文件，
 * 这里同时显式声明 LocaleResolver，默认语言 zh_CN，仅支持 zh_CN 与 en_US。
 * 真正的语言切换发生在 {@link com.maxkb4j.system.interceptor.LocaleInterceptor} 中：
 * 已登录用户使用 user 表中的 language 字段，未登录或未配置时默认 zh_CN。
 *
 * @author tarzan
 */
@Configuration
public class I18nConfig {

    /** 系统支持的语言列表 */
    public static final List<Locale> SUPPORTED_LOCALES = List.of(Locale.SIMPLIFIED_CHINESE, Locale.US);

    /** 默认语言：简体中文 */
    public static final Locale DEFAULT_LOCALE = Locale.SIMPLIFIED_CHINESE;

    @Bean
    public MessageSource messageSource(@Value("${spring.messages.basename:i18n/messages}") String basename) {
        return new YamlMessageSource(basename);
    }

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(DEFAULT_LOCALE);
        resolver.setSupportedLocales(SUPPORTED_LOCALES);
        return resolver;
    }

    private static class YamlMessageSource extends AbstractMessageSource {

        private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        private final String[] baseNames;
        private final Map<String, Properties> cachedProperties = new ConcurrentHashMap<>();

        private YamlMessageSource(String basename) {
            this.baseNames = StringUtils.commaDelimitedListToStringArray(basename);
        }

        @Override
        protected String resolveCodeWithoutArguments(@NotNull String code, @NotNull Locale locale) {
            return getMessage(code, locale);
        }

        @Override
        protected MessageFormat resolveCode(@NotNull String code, @NotNull Locale locale) {
            String message = getMessage(code, locale);
            return message == null ? null : createMessageFormat(message, locale);
        }

        private String getMessage(String code, Locale locale) {
            for (String basename : baseNames) {
                String message = getMergedProperties(basename.trim(), locale).getProperty(code);
                if (message != null) {
                    return message;
                }
            }
            return null;
        }

        private Properties getMergedProperties(String basename, Locale locale) {
            String cacheKey = basename + "_" + locale.toString();
            return cachedProperties.computeIfAbsent(cacheKey, key -> {
                Properties merged = new Properties();
                for (String filename : calculateFilenames(basename, locale)) {
                    loadProperties(merged, filename);
                }
                return merged;
            });
        }

        private List<String> calculateFilenames(String basename, Locale locale) {
            List<String> filenames = new ArrayList<>();
            filenames.add(basename);
            if (StringUtils.hasText(locale.getLanguage())) {
                String language = basename + "_" + locale.getLanguage();
                filenames.add(language);
                if (StringUtils.hasText(locale.getCountry())) {
                    String country = language + "_" + locale.getCountry();
                    filenames.add(country);
                    if (StringUtils.hasText(locale.getVariant())) {
                        filenames.add(country + "_" + locale.getVariant());
                    }
                }
            }
            return filenames;
        }

        private void loadProperties(Properties merged, String filename) {
            loadPropertiesFile(merged, filename + ".properties");
            loadYamlFile(merged, filename + ".yml");
            loadYamlFile(merged, filename + ".yaml");
        }

        private void loadPropertiesFile(Properties merged, String location) {
            try {
                Resource resource = resolver.getResource("classpath:" + location);
                if (resource.exists()) {
                    merged.putAll(PropertiesLoaderUtils.loadProperties(new EncodedResource(resource, StandardCharsets.UTF_8)));
                }
            } catch (Exception ignored) {
            }
        }

        private void loadYamlFile(Properties merged, String location) {
            Resource resource = resolver.getResource("classpath:" + location);
            if (!resource.exists()) {
                return;
            }
            YamlPropertiesFactoryBean factoryBean = new YamlPropertiesFactoryBean();
            factoryBean.setResources(resource);
            Properties properties = factoryBean.getObject();
            if (properties != null) {
                merged.putAll(properties);
            }
        }
    }
}
