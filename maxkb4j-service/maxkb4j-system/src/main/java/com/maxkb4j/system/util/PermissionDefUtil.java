package com.maxkb4j.system.util;

import com.maxkb4j.common.constant.AuthType;
import com.maxkb4j.common.constant.Permission;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    // 反向索引：由 PERMISSION_MAP 生成，保证单一数据源；重复定义会在类加载时直接报错
    private static final Map<PermissionKey, String> KEY_INDEX = PERMISSION_MAP.entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(
                    e -> permissionKey(e.getValue().authType(), e.getValue().permissionList()),
                    Map.Entry::getKey));

    private record PermissionKey(String authType, Set<String> permissions) {
    }

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
        return PERMISSION_MAP.getOrDefault(key, new PermissionDef(AuthType.RESOURCE_PERMISSION_GROUP, List.of(Permission.NOT_AUTH)));
    }

    /**
     * 根据 type 和 permissions 反向查找对应的 Map key
     * 权限列表按集合语义比较，忽略顺序、重复项与 null 元素
     *
     * @param authType       权限类型，如 "RESOURCE_PERMISSION_GROUP"
     * @param permissionList 权限列表，如 ["MANAGE", "VIEW"]
     * @return 匹配的 key，未找到时返回 {@link Permission#NOT_AUTH}
     */
    public static String findKey(String authType, List<String> permissionList) {
        if (authType == null || permissionList == null) {
            return Permission.NOT_AUTH;
        }
        return KEY_INDEX.getOrDefault(permissionKey(authType, permissionList), Permission.NOT_AUTH);
    }

    private static PermissionKey permissionKey(String authType, List<String> permissionList) {
        return new PermissionKey(authType, permissionList.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet()));
    }

}