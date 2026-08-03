package com.maxkb4j.core.support.permission;

import java.util.List;

/**
 * 携带数据权限范围的查询条件。
 *
 * <p>由各业务模块的 Query DTO 实现，配合 {@link DataPermissionSupport#fill} 注入当前用户的权限范围，
 * 供 Mapper XML 据此拼接 include/exclude 过滤（与原内联的 isAdmin/targetIds 语义一致）。
 *
 * @author tarzan
 */
public interface PermissionScopeAware {

    void setIsAdmin(Boolean isAdmin);

    void setTargetIds(List<String> targetIds);
}