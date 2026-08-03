package com.maxkb4j.tool.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.maxkb4j.core.support.permission.DataPermissionScope;
import com.maxkb4j.core.support.permission.DataPermissionSupport;
import com.maxkb4j.system.constant.AuthTargetType;
import com.maxkb4j.tool.entity.ToolEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 工具权限处理器：根据当前登录用户角色，向查询 wrapper 上叠加可见性过滤。
 *
 * <p>ADMIN 不限制；USER 仅可见有授权的资源；其余角色（含无角色）视为无权限，
 * 通过追加 {@code limit 0} 短路查询。
 *
 * @author tarzan
 */
@Component
@RequiredArgsConstructor
public class ToolPermissionHandler {

    private final DataPermissionSupport dataPermissionSupport;

    /**
     * 在 wrapper 上叠加角色过滤。
     */
    public void applyRoleFilter(LambdaQueryWrapper<ToolEntity> wrapper) {
        DataPermissionScope scope = dataPermissionSupport.resolve(AuthTargetType.TOOL);
        if (scope.isEmptyResult()) {
            wrapper.last(" limit 0");
            return;
        }
        if (!scope.isAdmin()) {
            wrapper.in(ToolEntity::getId, scope.getTargetIds());
        }
    }
}