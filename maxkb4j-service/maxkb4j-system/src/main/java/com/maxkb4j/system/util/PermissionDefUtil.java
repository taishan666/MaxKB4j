package com.maxkb4j.system.util;

import com.maxkb4j.common.constant.AuthType;
import com.maxkb4j.common.constant.Permission;

import java.util.List;
import java.util.Map;

/**
 * 权限定义工具类
 * 提供系统预置权限类型的查询与获取
 */
public final class PermissionDefUtil {


    // 预置权限映射表（不可变）
    private static final Map<String, PermissionDef> PERMISSION_MAP = Map.of(
            Permission.ROLE, new PermissionDef(AuthType.ROLE, List.of(Permission.ROLE)),
            Permission.MANAGE, new PermissionDef(AuthType.RESOURCE_PERMISSION_GROUP, List.of(Permission.MANAGE, Permission.VIEW)),
            Permission.VIEW, new PermissionDef(AuthType.RESOURCE_PERMISSION_GROUP, List.of(Permission.VIEW)),
            Permission.NOT_AUTH, new PermissionDef(AuthType.RESOURCE_PERMISSION_GROUP, List.of(Permission.NOT_AUTH))
    );

    // 私有构造器防止实例化
    private PermissionDefUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 根据 key 获取权限定义
     *
     * @param key 权限标识，如 "MANAGE"、"VIEW"
     * @return 对应的 PermissionDef，不存在时返回 Optional.empty()
     */
    public static PermissionDef get(String key) {
        return PERMISSION_MAP.getOrDefault(key,new PermissionDef(AuthType.RESOURCE_PERMISSION_GROUP, List.of(Permission.NOT_AUTH)));
    }

    /**
     * 根据 type 和 permissions 反向查找对应的 Map key
     *
     * @param authType        权限类型，如 "RESOURCE_PERMISSION_GROUP"
     * @param permissionList 权限列表，如 ["MANAGE", "VIEW"]
     * @return 匹配的 key，未找到时返回 Optional.empty()
     */
    public static String findKey(String authType, List<String> permissionList) {
        if (authType == null || permissionList == null) {
            return Permission.NOT_AUTH;
        }
        // List.of 保证顺序敏感匹配；如需无序匹配可改用 Set
        return PERMISSION_MAP.entrySet().stream()
                .filter(e -> e.getValue().authType().equals(authType)
                        && e.getValue().permissionList().equals(permissionList))
                .map(Map.Entry::getKey)
                .findFirst().orElse(Permission.NOT_AUTH);
    }

}