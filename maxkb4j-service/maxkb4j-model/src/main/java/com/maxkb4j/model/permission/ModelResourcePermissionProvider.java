package com.maxkb4j.model.permission;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maxkb4j.model.entity.ModelEntity;
import com.maxkb4j.model.mapper.ModelMapper;
import com.maxkb4j.system.constant.AuthTargetType;
import com.maxkb4j.user.service.IResourcePermissionPageProvider;
import com.maxkb4j.user.support.ResourcePermissionQuerySupport;
import com.maxkb4j.user.vo.UserResourcePermissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * 模型资源的权限分页查询实现。
 *
 * <p>实现 user-api 中的 {@link IResourcePermissionPageProvider} SPI，
 * 由 system 模块的 {@code UserResourcePermissionServiceImpl} 在运行期注入，
 * 避免 system 模块反向编译依赖 model。模型无独立 icon 字段，以 provider 作为展示图标来源。
 *
 * @author tarzan
 */
@Service
@RequiredArgsConstructor
public class ModelResourcePermissionProvider implements IResourcePermissionPageProvider {

    private final ModelMapper modelMapper;

    @Override
    public String supportType() {
        return AuthTargetType.MODEL;
    }

    @Override
    public IPage<UserResourcePermissionVO> pageResource(int current, int size, String name,
                                                       Map<String, String> permissionMap,
                                                       Set<String> permissionFilter) {
        return ResourcePermissionQuerySupport.pageResource(
                new Page<>(current, size), modelMapper,
                ModelEntity::getName, ModelEntity::getId,
                name, ModelEntity::getProvider,
                permissionMap, permissionFilter, supportType(), null);
    }
}
