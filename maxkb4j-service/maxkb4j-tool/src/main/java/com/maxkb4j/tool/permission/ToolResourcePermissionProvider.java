package com.maxkb4j.tool.permission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maxkb4j.system.constant.AuthTargetType;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.mapper.ToolMapper;
import com.maxkb4j.core.support.IResourcePermissionPageProvider;
import com.maxkb4j.core.support.ResourcePermissionQuerySupport;
import com.maxkb4j.core.support.vo.UserResourcePermissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 工具资源的权限分页查询实现。
 *
 * <p>实现 user-api 中的 {@link IResourcePermissionPageProvider} SPI，
 * 由 system 模块的 {@code UserResourcePermissionServiceImpl} 在运行期注入，
 * 避免 system 模块反向编译依赖 tool。仅查询工作空间级（scope=WORKSPACE）工具。
 *
 * @author tarzan
 */
@Service
@RequiredArgsConstructor
public class ToolResourcePermissionProvider implements IResourcePermissionPageProvider {

    private final ToolMapper toolMapper;

    @Override
    public String supportType() {
        return AuthTargetType.TOOL;
    }

    @Override
    public IPage<UserResourcePermissionVO> pageResource(int current, int size, String name,
                                                       Map<String, String> permissionMap,
                                                       Set<String> permissionFilter) {
        Consumer<LambdaQueryWrapper<ToolEntity>> scopeFilter = w -> w.eq(ToolEntity::getScope, "WORKSPACE");
        return ResourcePermissionQuerySupport.pageResource(
                new Page<>(current, size), toolMapper,
                ToolEntity::getName, ToolEntity::getId,
                name, ToolEntity::getIcon,
                permissionMap, permissionFilter, supportType(), scopeFilter);
    }
}
