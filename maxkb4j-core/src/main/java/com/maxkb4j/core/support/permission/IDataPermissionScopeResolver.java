package com.maxkb4j.core.support.permission;

/**
 * 当前登录用户对某类资源的数据权限范围解析器。
 *
 * <p>接口位于 core 供各业务模块（应用/工具/知识库/模型）注入使用，
 * 实现位于 system 模块：依据当前用户角色解析，管理员不限制；
 * 普通用户仅可见已授权 targetIds；无角色或无授权时无可见资源。
 *
 * @author tarzan
 */
public interface IDataPermissionScopeResolver {

    /**
     * 解析当前登录用户对指定资源类型的数据权限范围。
     *
     * @param authTargetType 资源类型，对应 {@code AuthTargetType} 常量值
     */
    DataPermissionScope resolve(String authTargetType);

    /**
     * 解析当前登录用户对指定资源类型的「管理」数据权限范围（删除等写操作的资源级校验用）。
     *
     * <p>与 {@link #resolve(String)} 区别：仅收集授权列表中包含 MANAGE 的资源。
     *
     * @param authTargetType 资源类型，对应 {@code AuthTargetType} 常量值
     */
    DataPermissionScope resolveManageScope(String authTargetType);
}
