package com.maxkb4j.core.support;

import com.maxkb4j.common.constant.Permission;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.maxkb4j.core.support.ResourcePermissionQuerySupport.PermissionIdFilter;
import static com.maxkb4j.core.support.ResourcePermissionQuerySupport.buildPermissionIdFilter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ResourcePermissionQuerySupport#buildPermissionIdFilter(Map, Set)} 过滤分支测试。
 * 重点覆盖 NOT_AUTH 分支的兜底语义：解析值不是明确 NOT_AUTH 的记录一律视为已授权（排除），
 * 防止 ROLE/null 等未识别权限值被误判为未授权（缺陷扫描报告 M10）。
 */
class ResourcePermissionQuerySupportTest {

    @Test
    void emptyFilterReturnsNone() {
        PermissionIdFilter filter = buildPermissionIdFilter(Map.of("a", Permission.VIEW), Set.of());
        assertEquals(PermissionIdFilter.none(), filter);
    }

    @Test
    void unknownOnlyFilterReturnsEmptyResult() {
        PermissionIdFilter filter = buildPermissionIdFilter(Map.of("a", Permission.VIEW), Set.of(Permission.ROLE));
        assertTrue(filter.emptyResult());
    }

    @Test
    void positiveFilterIncludesMatchingIdsOnly() {
        Map<String, String> map = new HashMap<>();
        map.put("manage", Permission.MANAGE);
        map.put("view", Permission.VIEW);
        map.put("revoked", Permission.NOT_AUTH);
        PermissionIdFilter filter = buildPermissionIdFilter(map, Set.of(Permission.VIEW));
        assertFalse(filter.emptyResult());
        assertEquals(Set.of("view"), filter.includeIds());
        assertTrue(filter.excludeIds().isEmpty());
    }

    @Test
    void positiveFilterWithoutMatchReturnsEmptyResult() {
        PermissionIdFilter filter = buildPermissionIdFilter(Map.of("a", Permission.MANAGE), Set.of(Permission.VIEW));
        assertEquals(PermissionIdFilter.empty(), filter);
    }

    @Test
    void mixedPositiveAndNotAuthReturnsNone() {
        PermissionIdFilter filter = buildPermissionIdFilter(Map.of("a", Permission.VIEW),
                Set.of(Permission.VIEW, Permission.NOT_AUTH));
        assertEquals(PermissionIdFilter.none(), filter);
    }

    @Test
    void notAuthFilterExcludesAuthorizedIdsOnly() {
        Map<String, String> map = new HashMap<>();
        map.put("manage", Permission.MANAGE);
        map.put("view", Permission.VIEW);
        map.put("revoked", Permission.NOT_AUTH);
        PermissionIdFilter filter = buildPermissionIdFilter(map, Set.of(Permission.NOT_AUTH));
        assertFalse(filter.emptyResult());
        assertEquals(Set.of("manage", "view"), filter.excludeIds());
        assertTrue(filter.includeIds().isEmpty());
    }

    @Test
    void notAuthFilterTreatsUnrecognizedValuesAsAuthorized() {
        // M10 回归：ROLE/null/未来新增的权限值不得混入未授权结果，一律按已授权排除
        Map<String, String> map = new HashMap<>();
        map.put("role", Permission.ROLE);
        map.put("nullValue", null);
        map.put("future", "USE");
        map.put("revoked", Permission.NOT_AUTH);
        PermissionIdFilter filter = buildPermissionIdFilter(map, Set.of(Permission.NOT_AUTH));
        assertFalse(filter.emptyResult());
        assertEquals(Set.of("role", "nullValue", "future"), filter.excludeIds());
    }

    @Test
    void notAuthFilterWithEmptyMapExcludesNothing() {
        PermissionIdFilter filter = buildPermissionIdFilter(Map.of(), Set.of(Permission.NOT_AUTH));
        assertFalse(filter.emptyResult());
        assertTrue(filter.excludeIds().isEmpty());
    }
}
