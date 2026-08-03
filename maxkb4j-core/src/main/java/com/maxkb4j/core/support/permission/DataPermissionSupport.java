package com.maxkb4j.core.support.permission;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 数据权限范围注入的统一入口。
 *
 * <p>委托 {@link IDataPermissionScopeResolver} 解析当前登录用户的数据权限范围，
 * 供各业务模块（应用/工具/知识库/模型）在分页/列表查询前一次性注入，
 * 避免重复内联角色判断与授权 ID 收集逻辑。
 *
 * @author tarzan
 */
@Component
@RequiredArgsConstructor
public class DataPermissionSupport {

    private final IDataPermissionScopeResolver scopeResolver;

    /** 解析当前登录用户对指定资源类型的数据权限范围。 */
    public DataPermissionScope resolve(String authTargetType) {
        return scopeResolver.resolve(authTargetType);
    }

    /** 将当前用户的数据权限范围注入查询条件，供 Mapper XML 按 isAdmin/targetIds 拼接过滤。 */
    public void fill(PermissionScopeAware query, String authTargetType) {
        DataPermissionScope scope = scopeResolver.resolve(authTargetType);
        query.setIsAdmin(scope.isAdmin());
        query.setTargetIds(scope.getTargetIds());
    }
}