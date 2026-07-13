package com.maxkb4j.knowledge.permission;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maxkb4j.knowledge.entity.KnowledgeEntity;
import com.maxkb4j.knowledge.mapper.KnowledgeMapper;
import com.maxkb4j.system.constant.AuthTargetType;
import com.maxkb4j.user.service.IResourcePermissionPageProvider;
import com.maxkb4j.user.support.ResourcePermissionQuerySupport;
import com.maxkb4j.user.vo.UserResourcePermissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * 知识库资源的权限分页查询实现。
 *
 * <p>实现 user-api 中的 {@link IResourcePermissionPageProvider} SPI，
 * 由 system 模块的 {@code UserResourcePermissionServiceImpl} 在运行期注入，
 * 避免 system 模块反向编译依赖 knowledge。知识库无图标字段，icon 取空串。
 *
 * @author tarzan
 */
@Service
@RequiredArgsConstructor
public class KnowledgeResourcePermissionProvider implements IResourcePermissionPageProvider {

    private final KnowledgeMapper knowledgeMapper;

    @Override
    public String supportType() {
        return AuthTargetType.KNOWLEDGE;
    }

    @Override
    public IPage<UserResourcePermissionVO> pageResource(int current, int size, String name,
                                                       Map<String, String> permissionMap,
                                                       Set<String> permissionFilter) {
        return ResourcePermissionQuerySupport.pageResource(
                new Page<>(current, size), knowledgeMapper,
                KnowledgeEntity::getName, KnowledgeEntity::getId,
                name, e -> "",
                permissionMap, permissionFilter, supportType(), null);
    }
}
