package com.maxkb4j.tool.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.maxkb4j.common.constant.RoleType;
import com.maxkb4j.common.util.StpKit;
import com.maxkb4j.system.constant.AuthTargetType;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.user.service.IUserResourcePermissionService;
import com.maxkb4j.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;

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

    private final IUserService userService;
    private final IUserResourcePermissionService userResourcePermissionService;

    /**
     * 在 wrapper 上叠加角色过滤。
     */
    public void applyRoleFilter(LambdaQueryWrapper<ToolEntity> wrapper) {
        String loginId = StpKit.ADMIN.getLoginIdAsString();
        Set<String> roles = userService.getRoleById(loginId);
        if (CollectionUtils.isEmpty(roles)) {
            wrapper.last(" limit 0");
            return;
        }
        if (roles.contains(RoleType.ADMIN)) {
            return;
        }
        if (roles.contains(RoleType.USER)) {
            List<String> targetIds = userResourcePermissionService.getTargetIds(AuthTargetType.TOOL, loginId);
            if (CollectionUtils.isEmpty(targetIds)) {
                wrapper.last(" limit 0");
                return;
            }
            wrapper.in(ToolEntity::getId, targetIds);
            return;
        }
        wrapper.last(" limit 0");
    }
}
