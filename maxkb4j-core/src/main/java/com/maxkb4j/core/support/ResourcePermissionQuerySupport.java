package com.maxkb4j.core.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.constant.Permission;
import com.maxkb4j.common.mp.base.BaseEntity;
import com.maxkb4j.common.util.PageUtil;
import com.maxkb4j.core.support.vo.UserResourcePermissionVO;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 资源权限分页查询的通用支持逻辑。
 *
 * <p>将 {@code UserResourcePermissionServiceImpl} 中与具体业务实体无关的分页模板、
 * 权限过滤计算与权限 ID 过滤器抽离至此，供各业务模块的
 * {@code IResourcePermissionPageProvider} 实现复用，
 * 避免在各 provider 中重复这段逻辑。
 *
 * @author tarzan
 */
public final class ResourcePermissionQuerySupport {

    private ResourcePermissionQuerySupport() {
    }

    /**
     * 分页查询某类资源，并按名称和权限过滤。
     *
     * @param iconGetter       资源图标的取值方式，不同资源类型图标来源不同（应用/工具取 icon，模型取 provider，知识库为空串）
     * @param extraConditions  额外的 wrapper 条件，可为 null
     */
    public static <E extends BaseEntity> IPage<UserResourcePermissionVO> pageResource(
            Page<E> page,
            BaseMapper<E> mapper,
            SFunction<E, String> nameField,
            SFunction<E, String> idField,
            String name,
            Function<E, String> iconGetter,
            Map<String, String> permissionMap,
            Set<String> permissionFilter,
            String type,
            Consumer<LambdaQueryWrapper<E>> extraConditions) {
        PermissionIdFilter filter = buildPermissionIdFilter(permissionMap, permissionFilter);
        if (filter.emptyResult()) {
            return new Page<>(page.getCurrent(), page.getSize(), 0);
        }
        LambdaQueryWrapper<E> wrapper = Wrappers.<E>lambdaQuery()
                .like(StringUtils.isNotBlank(name), nameField, name);
        if (CollectionUtils.isNotEmpty(filter.includeIds())) {
            wrapper.in(idField, filter.includeIds());
        }
        if (CollectionUtils.isNotEmpty(filter.excludeIds())) {
            wrapper.notIn(idField, filter.excludeIds());
        }
        if (extraConditions != null) {
            extraConditions.accept(wrapper);
        }
        mapper.selectPage(page, wrapper);
        return PageUtil.copy(page, e -> buildResourceVO(e.getId(), nameField.apply(e), iconGetter.apply(e), type, permissionMap.getOrDefault(e.getId(), Permission.NOT_AUTH)));
    }

    public static UserResourcePermissionVO buildResourceVO(String id, String name, String icon, String type, String permission) {
        UserResourcePermissionVO vo = new UserResourcePermissionVO();
        vo.setId(id);
        vo.setName(name);
        vo.setIcon(icon);
        vo.setFolderId(id);
        vo.setWorkspaceId(AppConst.DEFAULT_WORKSPACE_ID);
        vo.setAuthTargetType(type);
        vo.setPermission(permission);
        return vo;
    }

    /**
     * 根据权限过滤条件计算资源/用户 ID 的过滤方式。
     * <p>permissionMap 的 key 为已存在权限记录的 ID，value 为该记录解析后的权限（MANAGE/VIEW/NOT_AUTH）；
     * 不在 map 中的 ID 视为 NOT_AUTH（未授权）。
     * <ul>
     *   <li>过滤为空或同时包含正权限与 NOT_AUTH：不限制，返回全部；</li>
     *   <li>仅正权限：包含命中 ID，命中为空时返回空结果；</li>
     *   <li>仅 NOT_AUTH：排除所有已授权（VIEW/MANAGE）的 ID。</li>
     * </ul>
     */
    public static PermissionIdFilter buildPermissionIdFilter(Map<String, String> permissionMap, Set<String> filter) {
        if (filter.isEmpty()) {
            return PermissionIdFilter.none();
        }
        boolean includePositive = filter.contains(Permission.VIEW) || filter.contains(Permission.MANAGE);
        boolean includeNotAuth = filter.contains(Permission.NOT_AUTH);
        if (!includePositive && !includeNotAuth) {
            return PermissionIdFilter.empty();
        }
        if (includePositive && includeNotAuth) {
            return PermissionIdFilter.none();
        }
        if (includePositive) {
            Set<String> matchIds = permissionMap.entrySet().stream()
                    .filter(e -> filter.contains(e.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            return matchIds.isEmpty() ? PermissionIdFilter.empty() : new PermissionIdFilter(false, matchIds, Set.of());
        }
        // 仅 NOT_AUTH：排除所有已授权 ID（含权限列表为空但存在记录的项）
        Set<String> permittedIds = permissionMap.entrySet().stream()
                .filter(e -> Permission.VIEW.equals(e.getValue()) || Permission.MANAGE.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        return new PermissionIdFilter(false, Set.of(), permittedIds);
    }

    /**
     * 权限过滤结果：emptyResult 表示无可命中项需返回空页；includeIds/excludeIds 用于在分页查询中按 ID 收窄范围。
     */
    public record PermissionIdFilter(boolean emptyResult, Set<String> includeIds, Set<String> excludeIds) {
        static PermissionIdFilter none() {
            return new PermissionIdFilter(false, Set.of(), Set.of());
        }

        static PermissionIdFilter empty() {
            return new PermissionIdFilter(true, Set.of(), Set.of());
        }
    }
}
