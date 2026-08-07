package com.maxkb4j.system.service.impl;

import com.maxkb4j.common.constant.RoleType;
import com.maxkb4j.common.constant.Permission;
import com.maxkb4j.common.context.UserContext;
import com.maxkb4j.core.support.permission.DataPermissionScope;
import com.maxkb4j.core.support.permission.IDataPermissionScopeResolver;
import com.maxkb4j.user.service.IUserResourcePermissionService;
import com.maxkb4j.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;

/**
 * 数据权限范围解析器的默认实现。
 *
 * <ul>
 *   <li>无角色：无可见资源；</li>
 *   <li>ADMIN：不附加任何过滤；</li>
 *   <li>其余角色：仅可见已授权 targetIds，无授权则无可见资源。</li>
 * </ul>
 *
 * @author tarzan
 */
@Component
@RequiredArgsConstructor
public class DataPermissionScopeResolver implements IDataPermissionScopeResolver {

    private final UserContext userContext;
    private final IUserService userService;
    private final IUserResourcePermissionService userResourcePermissionService;

    @Override
    public DataPermissionScope resolve(String authTargetType) {
        return resolve(authTargetType, Permission.VIEW);
    }

    @Override
    public DataPermissionScope resolveManageScope(String authTargetType) {
        return resolve(authTargetType, Permission.MANAGE);
    }

    /** 按权限级别解析数据权限范围：VIEW 用于可见性过滤，MANAGE 用于删除等写操作校验。 */
    private DataPermissionScope resolve(String authTargetType, String permission) {
        String loginId = userContext.getUserId();
        Set<String> roles = userService.getRoleById(loginId);
        if (CollectionUtils.isEmpty(roles)) {
            return DataPermissionScope.empty();
        }
        if (roles.contains(RoleType.ADMIN)) {
            return DataPermissionScope.admin();
        }
        List<String> targetIds = userResourcePermissionService.getTargetIds(authTargetType, loginId, permission);
        return targetIds.isEmpty() ? DataPermissionScope.empty() : DataPermissionScope.limited(targetIds);
    }
}
