package com.tarzan.maxkb4j.module.system.permission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationMapper;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.KnowledgeEntity;
import com.tarzan.maxkb4j.module.knowledge.mapper.KnowledgeMapper;
import com.tarzan.maxkb4j.module.model.info.entity.ModelEntity;
import com.tarzan.maxkb4j.module.model.info.mapper.ModelMapper;
import com.tarzan.maxkb4j.module.system.permission.entity.UserResourcePermissionEntity;
import com.tarzan.maxkb4j.module.system.permission.mapper.UserResourcePermissionMapper;
import com.tarzan.maxkb4j.module.system.permission.vo.UserResourcePermissionVO;
import com.tarzan.maxkb4j.module.tool.domain.entity.ToolEntity;
import com.tarzan.maxkb4j.module.tool.mapper.ToolMapper;
import com.tarzan.maxkb4j.util.BeanUtil;
import com.tarzan.maxkb4j.util.PageUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class UserResourcePermissionService extends ServiceImpl<UserResourcePermissionMapper, UserResourcePermissionEntity> {

    private final ApplicationMapper applicationMapper;
    private final KnowledgeMapper datasetMapper;
    private final ToolMapper toolMapper;
    private final ModelMapper modelMapper;

    public boolean save(String type, String targetId, String userId, String workspaceId) {
        UserResourcePermissionEntity entity = new UserResourcePermissionEntity();
        entity.setAuthTargetType(type);
        entity.setTargetId(targetId);
        entity.setUserId(userId);
        entity.setPermissionList(List.of("VIEW", "MANAGE"));
        entity.setAuthType("RESOURCE_PERMISSION_GROUP");
        entity.setWorkspaceId(workspaceId);
        return this.save(entity);
    }


    public IPage<UserResourcePermissionVO> userResourcePermissionPage(String userId, String type, int current, int size) {
        final String DEFAULT_WORKSPACE_ID = "default";
        LambdaQueryWrapper<UserResourcePermissionEntity> wrapper = Wrappers.<UserResourcePermissionEntity>lambdaQuery()
                .eq(UserResourcePermissionEntity::getUserId, userId)
                .eq(UserResourcePermissionEntity::getAuthTargetType, type)
                .eq(UserResourcePermissionEntity::getWorkspaceId, DEFAULT_WORKSPACE_ID);
        List<UserResourcePermissionEntity> userResourcePermissions = baseMapper.selectList(wrapper);
        Map<String, List<String>> map = userResourcePermissions.stream().collect(Collectors.toMap(UserResourcePermissionEntity::getTargetId, UserResourcePermissionEntity::getPermissionList));
        switch (type) {
            case "APPLICATION":
                Page<ApplicationEntity> appPage = new Page<>(current, size);
                applicationMapper.selectPage(appPage, null);
                return PageUtil.copy(appPage, app -> {
                    UserResourcePermissionVO vo = new UserResourcePermissionVO();
                    vo.setId(app.getId());
                    vo.setName(app.getName());
                    vo.setIcon(app.getIcon());
                    vo.setFolderId(app.getId());
                    vo.setWorkspaceId(DEFAULT_WORKSPACE_ID);
                    vo.setAuthTargetType(type);
                    vo.setPermission(getPermissionFromList(map.get(app.getId())));
                    return vo;
                });
            case "KNOWLEDGE":
                Page<KnowledgeEntity> datasetPage = new Page<>(current, size);
                datasetMapper.selectPage(datasetPage, null);
                return PageUtil.copy(datasetPage, dataset -> {
                    UserResourcePermissionVO vo = new UserResourcePermissionVO();
                    vo.setId(dataset.getId());
                    vo.setName(dataset.getName());
                    vo.setIcon("");
                    vo.setFolderId(dataset.getId());
                    vo.setWorkspaceId(DEFAULT_WORKSPACE_ID);
                    vo.setAuthTargetType(type);
                    vo.setPermission(getPermissionFromList(map.get(dataset.getId())));
                    return vo;
                });
            case "TOOL":
                Page<ToolEntity> toolPage = new Page<>(current, size);
                toolMapper.selectPage(toolPage, null);
                return PageUtil.copy(toolPage, tool -> {
                    UserResourcePermissionVO vo = new UserResourcePermissionVO();
                    vo.setId(tool.getId());
                    vo.setName(tool.getName());
                    vo.setIcon(tool.getIcon());
                    vo.setFolderId(tool.getId());
                    vo.setWorkspaceId(DEFAULT_WORKSPACE_ID);
                    vo.setAuthTargetType(type);
                    vo.setPermission(getPermissionFromList(map.get(tool.getId())));
                    return vo;
                });
            case "MODEL":
                Page<ModelEntity> modelPage = new Page<>(current, size);
                modelMapper.selectPage(modelPage, null);
                return PageUtil.copy(modelPage, model -> {
                    UserResourcePermissionVO vo = new UserResourcePermissionVO();
                    vo.setId(model.getId());
                    vo.setName(model.getName());
                    vo.setIcon(model.getProvider());
                    vo.setFolderId(model.getId());
                    vo.setWorkspaceId(DEFAULT_WORKSPACE_ID);
                    vo.setAuthTargetType(type);
                    vo.setPermission(getPermissionFromList(map.get(model.getId())));
                    return vo;
                });
            default:
                return new Page<>(current, size);
        }
    }

    private String getPermissionFromList(List<String> permissionList) {
        if (CollectionUtils.isNotEmpty(permissionList)) {
            if (permissionList.contains("MANAGE")) {
                return "MANAGE";
            } else if (permissionList.contains("VIEW")) {
                return "VIEW";
            } else {
                return "NOT_AUTH";
            }
        } else {
            return "NOT_AUTH";
        }
    }


    @Transactional
    public boolean update(String userId, String type, List<UserResourcePermissionVO> list) {
        List<String> targetIds = list.stream().map(UserResourcePermissionVO::getTargetId).toList();
        this.remove(Wrappers.<UserResourcePermissionEntity>lambdaUpdate().eq(UserResourcePermissionEntity::getUserId, userId).eq(UserResourcePermissionEntity::getAuthTargetType, type).in(UserResourcePermissionEntity::getTargetId, targetIds));
        List<UserResourcePermissionEntity> saveList = list.stream().map(vo -> {
            if ("NOT_AUTH".equals(vo.getPermission())) {
                vo.setPermissionList(List.of());
            } else if ("MANAGE".equals(vo.getPermission())) {
                vo.setPermissionList(List.of("MANAGE", "VIEW"));
            } else if ("VIEW".equals(vo.getPermission())) {
                vo.setPermissionList(List.of("VIEW"));
            } else {
                vo.setPermissionList(List.of());
            }
            vo.setUserId(userId);
            vo.setAuthType("RESOURCE_PERMISSION_GROUP");
            vo.setWorkspaceId("default");
            vo.setAuthTargetType(type);
            return BeanUtil.copy(vo, UserResourcePermissionEntity.class);
        }).toList();
        return this.saveBatch(saveList);
    }

    public List<String> getTargetIds(String authTargetType, String userId) {
        List<UserResourcePermissionEntity> userResourcePermissions =this.lambdaQuery()
                .select(UserResourcePermissionEntity::getTargetId,UserResourcePermissionEntity::getPermissionList)
                .eq(UserResourcePermissionEntity::getUserId, userId)
                .eq(UserResourcePermissionEntity::getAuthTargetType, authTargetType).list();
        return userResourcePermissions.stream()
                .filter(permission -> permission.getPermissionList().contains("VIEW"))
                .map(UserResourcePermissionEntity::getTargetId).toList();
    }
}