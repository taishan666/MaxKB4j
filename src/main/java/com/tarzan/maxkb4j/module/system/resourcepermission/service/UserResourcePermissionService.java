package com.tarzan.maxkb4j.module.system.resourcepermission.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationMapper;
import com.tarzan.maxkb4j.module.dataset.domain.entity.DatasetEntity;
import com.tarzan.maxkb4j.module.dataset.mapper.DatasetMapper;
import com.tarzan.maxkb4j.module.model.info.entity.ModelEntity;
import com.tarzan.maxkb4j.module.model.info.mapper.ModelMapper;
import com.tarzan.maxkb4j.module.system.resourcepermission.entity.UserResourcePermissionEntity;
import com.tarzan.maxkb4j.module.system.resourcepermission.mapper.UserResourcePermissionMapper;
import com.tarzan.maxkb4j.module.system.resourcepermission.vo.UserResourcePermissionVO;
import com.tarzan.maxkb4j.module.tool.domain.entity.ToolEntity;
import com.tarzan.maxkb4j.module.tool.mapper.ToolMapper;
import com.tarzan.maxkb4j.util.PageUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@AllArgsConstructor
@Service
public class UserResourcePermissionService extends ServiceImpl<UserResourcePermissionMapper, UserResourcePermissionEntity> {

    private final ApplicationMapper applicationMapper;
    private final DatasetMapper datasetMapper;
    private final ToolMapper toolMapper;
    private final ModelMapper modelMapper;

    public boolean save(String type, String targetId, String userId, String workspaceId) {
        UserResourcePermissionEntity entity = new UserResourcePermissionEntity();
        entity.setAuthTargetType(type);
        entity.setTargetId(targetId);
        entity.setUserId(userId);
        entity.setPermissionList(Set.of("VIEW", "MANAGE"));
        entity.setAuthType("RESOURCE_PERMISSION_GROUP");
        entity.setWorkspaceId(workspaceId);
        return this.save(entity);
    }


    // ... existing code ...
    public IPage<UserResourcePermissionVO> userResourcePermissionPage(String userId, String type, int current, int size) {
        final String DEFAULT_WORKSPACE_ID = "default";
        final String NOT_AUTH_PERMISSION = "NOT_AUTH";
        switch (type) {
            case "APPLICATION":
                Page<ApplicationEntity> appPage = new Page<>(current, size);
                applicationMapper.selectPage(appPage, null);
                return PageUtil.copy(appPage, app -> {
                    UserResourcePermissionVO vo = new UserResourcePermissionVO();
                    vo.setName(app.getName());
                    vo.setIcon(app.getIcon());
                    vo.setFolderId(app.getId());
                    vo.setWorkspaceId(DEFAULT_WORKSPACE_ID);
                    vo.setAuthTargetType(type);
                    vo.setPermission(NOT_AUTH_PERMISSION);
                    return vo;
                });
            case "KNOWLEDGE":
                Page<DatasetEntity> datasetPage = new Page<>(current, size);
                datasetMapper.selectPage(datasetPage, null);
                return PageUtil.copy(datasetPage, dataset -> {
                    UserResourcePermissionVO vo = new UserResourcePermissionVO();
                    vo.setName(dataset.getName());
                    vo.setIcon("");
                    vo.setFolderId(dataset.getId());
                    vo.setWorkspaceId(DEFAULT_WORKSPACE_ID);
                    vo.setAuthTargetType(type);
                    vo.setPermission(NOT_AUTH_PERMISSION);
                    return vo;
                });
            case "TOOL":
                Page<ToolEntity> toolPage = new Page<>(current, size);
                toolMapper.selectPage(toolPage, null);
                return PageUtil.copy(toolPage, tool -> {
                    UserResourcePermissionVO vo = new UserResourcePermissionVO();
                    vo.setName(tool.getName());
                    vo.setIcon(tool.getIcon());
                    vo.setFolderId(tool.getId());
                    vo.setWorkspaceId(DEFAULT_WORKSPACE_ID);
                    vo.setAuthTargetType(type);
                    vo.setPermission(NOT_AUTH_PERMISSION);
                    return vo;
                });
            case "MODEL":
                Page<ModelEntity> modelPage = new Page<>(current, size);
                modelMapper.selectPage(modelPage, null);
                return PageUtil.copy(modelPage, model -> {
                    UserResourcePermissionVO vo = new UserResourcePermissionVO();
                    vo.setName(model.getName());
                    vo.setIcon(model.getProvider());
                    vo.setFolderId(model.getId());
                    vo.setWorkspaceId(DEFAULT_WORKSPACE_ID);
                    vo.setAuthTargetType(type);
                    vo.setPermission(NOT_AUTH_PERMISSION);
                    return vo;
                });
            default:
                return new Page<>(current, size);
        }
    }

    public boolean update(String userId, String type, UserResourcePermissionVO vo) {
        if ("NOT_AUTH".equals(vo.getPermission())) {
            vo.setPermissionList(Set.of());
        } else if ("MANAGE".equals(vo.getPermission())) {
            vo.setPermissionList(Set.of("MANAGE", "VIEW"));
        } else if ("VIEW".equals(vo.getPermission())) {
            vo.setPermissionList(Set.of("VIEW"));
        } else {
            vo.setPermissionList(Set.of());
        }
        return this.lambdaUpdate().set(UserResourcePermissionEntity::getPermissionList, vo.getPermissionList()).eq(UserResourcePermissionEntity::getUserId, userId).eq(UserResourcePermissionEntity::getTargetId, vo.getTargetId()).update();
    }
}