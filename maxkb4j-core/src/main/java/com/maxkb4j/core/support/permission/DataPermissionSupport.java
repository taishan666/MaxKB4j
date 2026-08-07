package com.maxkb4j.core.support.permission;

import com.maxkb4j.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

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

    /** 解析当前登录用户对指定资源类型的「管理」数据权限范围（删除等写操作的资源级校验用）。 */
    public DataPermissionScope resolveManageScope(String authTargetType) {
        return scopeResolver.resolveManageScope(authTargetType);
    }

    /**
     * 批量写操作（如批量删除）前的资源级权限校验：
     * 管理员放行全部目标；其余用户要求对每个目标资源均有 MANAGE 授权，
     * 存在无权限资源时整体拒绝并抛出业务异常。
     *
     * @param authTargetType 资源类型，对应 {@code AuthTargetType} 常量值
     * @param targetIds      待操作的目标资源 ID 列表；为空直接放行
     * @throws ApiException 存在无管理权限的资源时抛出
     */
    public void checkManagePermission(String authTargetType, List<String> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) {
            return;
        }
        DataPermissionScope scope = scopeResolver.resolveManageScope(authTargetType);
        if (scope.isAdmin()) {
            return;
        }
        if (!scope.getTargetIds().containsAll(targetIds)) {
            throw new ApiException("resource.no.permission");
        }
    }

    /** 将当前用户的数据权限范围注入查询条件，供 Mapper XML 按 isAdmin/targetIds 拼接过滤。 */
    public void fill(PermissionScopeAware query, String authTargetType) {
        DataPermissionScope scope = scopeResolver.resolve(authTargetType);
        query.setIsAdmin(scope.isAdmin());
        query.setTargetIds(scope.getTargetIds());
    }
}
