package com.maxkb4j.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在 Controller 方法参数上,由 {@code CurrentUserIdArgumentResolver} 注入当前登录用户ID。
 * <p>替代业务 Controller 中直接调用 {@code StpKit.ADMIN.getLoginIdAsString()} 取当前用户,
 * 使 Controller 可脱离 sa-token 进行测试。
 *
 * @author tarzan
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserId {
}
