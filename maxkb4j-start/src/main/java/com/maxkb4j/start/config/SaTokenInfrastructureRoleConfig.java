package com.maxkb4j.start.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

/**
 * 将 Sa-Token 自带的几个 advisor bean 标记为 {@link BeanDefinition#ROLE_INFRASTRUCTURE}，
 * 避免 Spring 在容器启动期打印如下警告：
 *
 * <pre>
 *   Bean 'cn.dev33.satoken.aop.SaAopPointcutAdvisorBeanRegister' ... is not eligible for
 *   getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying).
 *   Is this bean getting eagerly injected/applied to a currently created BeanPostProcessor
 *   [projectingArgumentResolverBeanPostProcessor]?
 * </pre>
 *
 * <p>触发链路：spring-boot-starter-data-mongodb 引入 spring-data-commons，
 * 后者注册的 {@code ProjectingArgumentResolverBeanPostProcessor} 在创建期触发 AspectJ
 * AutoProxyCreator 枚举所有 Advisor bean，从而强行提前实例化 Sa-Token 的 advisor。
 * Sa-Token 的这些 advisor 是 AOP 基础设施，本应标为 INFRASTRUCTURE，
 * Spring 的 {@code BeanPostProcessorChecker} 对 infrastructure bean 不输出该警告。</p>
 *
 * <p>仅修改 bean 角色，不改变其行为，对鉴权切面功能无影响。</p>
 */
@Slf4j
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class SaTokenInfrastructureRoleConfig implements BeanFactoryPostProcessor {

    /**
     * 需要标记为基础设施角色的 Sa-Token bean 名称。
     * 这些 bean 由 Sa-Token 通过 {@code @Import} / {@code ImportBeanDefinitionRegistrar} 注册，
     * 名称固定。
     */
    private static final String[] SA_TOKEN_INFRA_BEAN_NAMES = {
            "cn.dev33.satoken.aop.SaAopPointcutAdvisorBeanRegister",
            "saAroundAnnotationHandlePointcutAdvisor"
    };

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        for (String beanName : SA_TOKEN_INFRA_BEAN_NAMES) {
            if (!beanFactory.containsBeanDefinition(beanName)) {
                continue;
            }
            BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
            if (bd.getRole() != BeanDefinition.ROLE_INFRASTRUCTURE) {
                bd.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
                log.debug("Marked Sa-Token bean '{}' as ROLE_INFRASTRUCTURE", beanName);
            }
        }
    }
}
