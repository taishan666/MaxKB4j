package com.tarzan.maxkb4j.module.system.permission.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.common.util.BeanUtil;
import com.tarzan.maxkb4j.common.util.PageUtil;
import com.tarzan.maxkb4j.common.util.StpKit;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationMapper;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.KnowledgeEntity;
import com.tarzan.maxkb4j.module.knowledge.mapper.KnowledgeMapper;
import com.tarzan.maxkb4j.module.model.info.entity.ModelEntity;
import com.tarzan.maxkb4j.module.model.info.mapper.ModelMapper;
import com.tarzan.maxkb4j.module.system.permission.constant.AuthTargetType;
import com.tarzan.maxkb4j.module.system.permission.entity.UserResourcePermissionEntity;
import com.tarzan.maxkb4j.module.system.permission.mapper.UserResourcePermissionMapper;
import com.tarzan.maxkb4j.module.system.permission.vo.ResourceUserPermissionVO;
import com.tarzan.maxkb4j.module.system.permission.vo.UserResourcePermissionVO;
import com.tarzan.maxkb4j.module.system.user.domain.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.mapper.UserMapper;
import com.tarzan.maxkb4j.module.tool.domain.entity.ToolEntity;
import com.tarzan.maxkb4j.module.tool.mapper.ToolMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class UserResourcePermissionService extends ServiceImpl<UserResourcePermissionMapper, UserResourcePermissionEntity> {
    private final String DEFAULT_ID = "default";
    private final ApplicationMapper applicationMapper;
    private final KnowledgeMapper datasetMapper;
    private final ToolMapper toolMapper;
    private final ModelMapper modelMapper;
    private final UserMapper userMapper;

    public boolean ownerSave(String type, String targetId, String userId) {
        UserResourcePermissionEntity entity = new UserResourcePermissionEntity();
        entity.setAuthTargetType(type);
        entity.setTargetId(targetId);
        entity.setUserId(userId);
        entity.setPermissionList(List.of("VIEW", "MANAGE"));
        entity.setAuthType("RESOURCE_PERMISSION_GROUP");
        entity.setWorkspaceId(DEFAULT_ID);
        return this.save(entity);
    }


    public boolean remove(String type, String targetId) {
        return this.lambdaUpdate().eq(UserResourcePermissionEntity::getAuthTargetType, type).eq(UserResourcePermissionEntity::getTargetId, targetId).eq(UserResourcePermissionEntity::getWorkspaceId, DEFAULT_ID).remove();
    }

    public boolean update(String type, String targetId, String userId) {
        return this.lambdaUpdate()
                .eq(UserResourcePermissionEntity::getAuthTargetType, type).eq(UserResourcePermissionEntity::getTargetId, targetId).eq(UserResourcePermissionEntity::getUserId, userId).eq(UserResourcePermissionEntity::getWorkspaceId, DEFAULT_ID).update();
    }


    public IPage<UserResourcePermissionVO> userResourcePermissionPage(String userId, String type, int current, int size) {
        LambdaQueryWrapper<UserResourcePermissionEntity> wrapper = Wrappers.<UserResourcePermissionEntity>lambdaQuery()
                .eq(UserResourcePermissionEntity::getUserId, userId)
                .eq(UserResourcePermissionEntity::getAuthTargetType, type)
                .eq(UserResourcePermissionEntity::getWorkspaceId, DEFAULT_ID);
        List<UserResourcePermissionEntity> userResourcePermissions = baseMapper.selectList(wrapper);
        Map<String, List<String>> map = userResourcePermissions.stream().collect(Collectors.toMap(UserResourcePermissionEntity::getTargetId, UserResourcePermissionEntity::getPermissionList));
        switch (type) {
            case AuthTargetType.APPLICATION:
                Page<ApplicationEntity> appPage = new Page<>(current, size);
                applicationMapper.selectPage(appPage, null);
                return PageUtil.copy(appPage, app -> {
                    UserResourcePermissionVO vo = new UserResourcePermissionVO();
                    vo.setId(app.getId());
                    vo.setName(app.getName());
                    vo.setIcon(app.getIcon());
                    vo.setFolderId(app.getId());
                    vo.setWorkspaceId(DEFAULT_ID);
                    vo.setAuthTargetType(type);
                    vo.setPermission(getPermissionFromList(map.get(app.getId())));
                    return vo;
                });
            case AuthTargetType.KNOWLEDGE:
                Page<KnowledgeEntity> datasetPage = new Page<>(current, size);
                datasetMapper.selectPage(datasetPage, null);
                return PageUtil.copy(datasetPage, dataset -> {
                    UserResourcePermissionVO vo = new UserResourcePermissionVO();
                    vo.setId(dataset.getId());
                    vo.setName(dataset.getName());
                    vo.setIcon("");
                    vo.setFolderId(dataset.getId());
                    vo.setWorkspaceId(DEFAULT_ID);
                    vo.setAuthTargetType(type);
                    vo.setPermission(getPermissionFromList(map.get(dataset.getId())));
                    return vo;
                });
            case AuthTargetType.TOOL:
                Page<ToolEntity> toolPage = new Page<>(current, size);
                Wrapper<ToolEntity> toolWrapper = Wrappers.<ToolEntity>lambdaQuery().eq(ToolEntity::getScope, "WORKSPACE");
                toolMapper.selectPage(toolPage, toolWrapper);
                return PageUtil.copy(toolPage, tool -> {
                    UserResourcePermissionVO vo = new UserResourcePermissionVO();
                    vo.setId(tool.getId());
                    vo.setName(tool.getName());
                    vo.setIcon(tool.getIcon());
                    vo.setFolderId(tool.getId());
                    vo.setWorkspaceId(DEFAULT_ID);
                    vo.setAuthTargetType(type);
                    vo.setPermission(getPermissionFromList(map.get(tool.getId())));
                    return vo;
                });
            case AuthTargetType.MODEL:
                Page<ModelEntity> modelPage = new Page<>(current, size);
                modelMapper.selectPage(modelPage, null);
                return PageUtil.copy(modelPage, model -> {
                    UserResourcePermissionVO vo = new UserResourcePermissionVO();
                    vo.setId(model.getId());
                    vo.setName(model.getName());
                    vo.setIcon(model.getProvider());
                    vo.setFolderId(model.getId());
                    vo.setWorkspaceId(DEFAULT_ID);
                    vo.setAuthTargetType(type);
                    vo.setPermission(getPermissionFromList(map.get(model.getId())));
                    return vo;
                });
            default:
                return new Page<>(current, size);
        }
    }
    public IPage<ResourceUserPermissionVO> resourceUserPermissionPage(String resourceId, String type, int current, int size) {
        LambdaQueryWrapper<UserResourcePermissionEntity> wrapper = Wrappers.<UserResourcePermissionEntity>lambdaQuery()
                .eq(UserResourcePermissionEntity::getTargetId, resourceId)
                .eq(UserResourcePermissionEntity::getAuthTargetType, type)
                .eq(UserResourcePermissionEntity::getWorkspaceId, DEFAULT_ID);
        List<UserResourcePermissionEntity> list = baseMapper.selectList(wrapper);
        Map<String, UserResourcePermissionEntity> map = list.stream()
                .collect(Collectors.toMap(
                        UserResourcePermissionEntity::getUserId,
                        e -> e,
                        (existing, replacement) -> existing // 保留第一个，也可选 replacement 保留最后一个
                ));
        String userId = StpKit.ADMIN.getLoginIdAsString();
        LambdaQueryWrapper<UserEntity>  userWrapper = Wrappers.<UserEntity>lambdaQuery()
                .in(UserEntity::getIsActive,true)
                .ne(UserEntity::getId, userId).orderByAsc(UserEntity::getCreateTime);
        Page<UserEntity> userPage = new Page<>(current, size);
        userPage= userMapper.selectPage(userPage,userWrapper);
        return PageUtil.copy(userPage, e -> {
            ResourceUserPermissionVO vo = new ResourceUserPermissionVO();
            UserResourcePermissionEntity permission = map.get(e.getId());
            vo.setId(e.getId());
            vo.setNickname(e.getNickname());
            vo.setUsername(e.getUsername());
            if (permission != null){
                vo.setPermission(getPermissionFromList(permission.getPermissionList()));
            }else{
                vo.setPermission("NOT_AUTH");
            }
            vo.setWorkspaceId(DEFAULT_ID);
            vo.setAuthTargetType(type);
            return vo;
        });
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
    public boolean userPermissionUpdate(String userId, String type, List<UserResourcePermissionVO> list) {
        List<String> targetIds = list.stream().map(UserResourcePermissionVO::getTargetId).toList();
        this.remove(Wrappers.<UserResourcePermissionEntity>lambdaUpdate().eq(UserResourcePermissionEntity::getUserId, userId).eq(UserResourcePermissionEntity::getAuthTargetType, type).in(UserResourcePermissionEntity::getTargetId, targetIds));
        List<UserResourcePermissionEntity> saveList = list.stream().map(vo -> {
            vo.setPermissionList(getPermissionFromList(vo.getPermission()));
            vo.setUserId(userId);
            vo.setAuthType("RESOURCE_PERMISSION_GROUP");
            vo.setWorkspaceId(DEFAULT_ID);
            vo.setAuthTargetType(type);
            vo.setFolderId(DEFAULT_ID);
            return BeanUtil.copy(vo, UserResourcePermissionEntity.class);
        }).toList();
        return this.saveBatch(saveList);
    }

    @Transactional
    public boolean resourcePermissionUpdate(String resourceId, String type, List<ResourceUserPermissionVO> list) {
        List<String> userIds = list.stream().map(ResourceUserPermissionVO::getUserId).toList();
        this.remove(Wrappers.<UserResourcePermissionEntity>lambdaUpdate().eq(UserResourcePermissionEntity::getTargetId, resourceId).eq(UserResourcePermissionEntity::getAuthTargetType, type).in(UserResourcePermissionEntity::getUserId, userIds));
        List<UserResourcePermissionEntity> saveList = list.stream().map(vo -> {
            vo.setPermissionList(getPermissionFromList(vo.getPermission()));
            vo.setTargetId(resourceId);
            vo.setAuthType("RESOURCE_PERMISSION_GROUP");
            vo.setWorkspaceId(DEFAULT_ID);
            vo.setAuthTargetType(type);
            return BeanUtil.copy(vo, UserResourcePermissionEntity.class);
        }).toList();
        return this.saveBatch(saveList);
    }

    private List<String>  getPermissionFromList(String permission) {
        if ("MANAGE".equals(permission)) {
            return List.of("MANAGE", "VIEW");
        } else if ("VIEW".equals(permission)) {
            return List.of("VIEW");
        }
        return List.of();
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


    public List<UserResourcePermissionEntity> getByUserId(String userId) {
        return this.lambdaQuery().eq(UserResourcePermissionEntity::getUserId, userId).list();
    }
}