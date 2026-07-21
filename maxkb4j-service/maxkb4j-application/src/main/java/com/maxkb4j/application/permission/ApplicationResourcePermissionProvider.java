package com.maxkb4j.application.permission;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.application.mapper.ApplicationMapper;
import com.maxkb4j.system.constant.AuthTargetType;
import com.maxkb4j.core.support.IResourcePermissionPageProvider;
import com.maxkb4j.core.support.ResourcePermissionQuerySupport;
import com.maxkb4j.core.support.vo.UserResourcePermissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * 应用资源的权限分页查询实现。
 *
 * <p>实现 user-api 中的 {@link IResourcePermissionPageProvider} SPI，
 * 由 system 模块的 {@code UserResourcePermissionServiceImpl} 在运行期注入，
 * 避免 system 模块反向编译依赖 application。
 *
 * @author tarzan
 */
@Service
@RequiredArgsConstructor
public class ApplicationResourcePermissionProvider implements IResourcePermissionPageProvider {

    private final ApplicationMapper applicationMapper;

    @Override
    public String supportType() {
        return AuthTargetType.APPLICATION;
    }

    @Override
    public IPage<UserResourcePermissionVO> pageResource(int current, int size, String name,
                                                       Map<String, String> permissionMap,
                                                       Set<String> permissionFilter) {
        return ResourcePermissionQuerySupport.pageResource(
                new Page<>(current, size), applicationMapper,
                ApplicationEntity::getName, ApplicationEntity::getId,
                name, ApplicationEntity::getIcon,
                permissionMap, permissionFilter, supportType(), null);
    }
}
