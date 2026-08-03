package com.maxkb4j.core.support.permission;

import java.util.List;

/**
 * 当前用户对某类资源的数据权限范围。
 *
 * <p>由 {@link IDataPermissionScopeResolver} 解析一次后，可在分页/列表查询中复用，
 * 避免各业务模块（应用/工具/知识库/模型）重复内联角色判断与授权 ID 收集逻辑。
 *
 * <p>三种状态：
 * <ul>
 *   <li>{@link #admin()} -- 管理员，不附加任何过滤；</li>
 *   <li>{@link #limited(List)} -- 普通用户，仅可见授权 ID 列表内的资源；</li>
 *   <li>{@link #empty()} -- 无可见资源（无角色或无授权），查询应返回空结果。</li>
 * </ul>
 *
 * @author tarzan
 */
public final class DataPermissionScope {

    private final boolean admin;
    private final List<String> targetIds;

    private DataPermissionScope(boolean admin, List<String> targetIds) {
        this.admin = admin;
        this.targetIds = targetIds == null ? List.of() : List.copyOf(targetIds);
    }

    /** 管理员：可见全部，不附加过滤。 */
    public static DataPermissionScope admin() {
        return new DataPermissionScope(true, List.of());
    }

    /** 普通用户：仅可见 {@code targetIds} 指定的资源；为空表示无任何授权。 */
    public static DataPermissionScope limited(List<String> targetIds) {
        return new DataPermissionScope(false, targetIds);
    }

    /** 无可见资源。 */
    public static DataPermissionScope empty() {
        return new DataPermissionScope(false, List.of());
    }

    public boolean isAdmin() {
        return admin;
    }

    public List<String> getTargetIds() {
        return targetIds;
    }

    /** 非管理员且无可授权资源时，查询应返回空结果。 */
    public boolean isEmptyResult() {
        return !admin && targetIds.isEmpty();
    }
}