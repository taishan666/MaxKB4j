package com.maxkb4j.common.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SpringUtil implements ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(SpringUtil.class);
    private static ConfigurableApplicationContext context;

    public SpringUtil() {
    }

    public static <T> Map<String, T> getBeansOfType(Class<T> type) {
        return getBeanFactory().getBeansOfType(type);
    }

    public static DefaultListableBeanFactory getBeanFactory() {
        if (context == null) {
            throw new IllegalStateException("ApplicationContext尚未注入，无法获取BeanFactory");
        }
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        if (beanFactory instanceof DefaultListableBeanFactory defaultListableBeanFactory) {
            return defaultListableBeanFactory;
        }
        throw new IllegalStateException("当前上下文的BeanFactory类型不支持该操作: "
                + (beanFactory != null ? beanFactory.getClass().getName() : "null"));
    }

    public static <T> T getBean(Class<T> clazz) {
        return clazz == null ? null : context.getBean(clazz);
    }

    public static <T> T getBean(String beanId) {
        return beanId == null ? null : (T) context.getBean(beanId);
    }

    public static <T> T getBean(String beanName, Class<T> clazz) {
        if (null != beanName && !"".equals(beanName.trim())) {
            return clazz == null ? null : context.getBean(beanName, clazz);
        } else {
            return null;
        }
    }

    public static ApplicationContext getContext() {
        return context == null ? null : context;
    }

    public static void publishEvent(ApplicationEvent event) {
        if (context != null) {
            try {
                context.publishEvent(event);
            } catch (Exception var2) {
                Exception ex = var2;
                log.error("Failed to publish event: {}", ex.getMessage(), ex);
            }

        }
    }

    public static void registerBeanDefinition(String beanName, BeanDefinition definition) {
        getBeanFactory().registerBeanDefinition(beanName, definition);
    }

    @Override
    public void setApplicationContext(@Nullable ApplicationContext context) throws BeansException {
        SpringUtil.context = (ConfigurableApplicationContext) context;
    }
}
