package com.tarzan.maxkb4j.module.model.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.model.entity.ModelEntity;
import com.tarzan.maxkb4j.module.model.mapper.ModelMapper;
import com.tarzan.maxkb4j.module.model.provider.ModelManage;
import com.tarzan.maxkb4j.module.model.vo.ModelVO;
import com.tarzan.maxkb4j.module.system.setting.entity.SystemSettingEntity;
import com.tarzan.maxkb4j.module.system.setting.service.SystemSettingService;
import com.tarzan.maxkb4j.module.system.user.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.service.UserService;
import com.tarzan.maxkb4j.util.BeanUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author tarzan
 * @date 2024-12-25 12:22:22
 */
@Service
public class ModelService extends ServiceImpl<ModelMapper, ModelEntity> {

    @Autowired
    private SystemSettingService systemSettingService;
    @Autowired
    private UserService userService;

    public List<ModelEntity> getUserIdAndType(UUID userId, String modelType) {
        modelType = StringUtils.isBlank(modelType) ? "LLM" : modelType;
        return this.list(Wrappers.<ModelEntity>lambdaQuery().eq(ModelEntity::getUserId, userId).eq(ModelEntity::getModelType, modelType));
    }

    public List<ModelEntity> models(String modelType) {
        modelType = StringUtils.isBlank(modelType) ? "LLM" : modelType;
        return baseMapper.selectList(Wrappers.<ModelEntity>lambdaQuery().eq(ModelEntity::getModelType, modelType));
    }

    public List<ModelVO> models(String name, String createUser, String permissionType, String modelType) {
        List<UserEntity> users = userService.lambdaQuery().list();
        Map<UUID, String> userMap = users.stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getUsername));
        LambdaQueryWrapper<ModelEntity> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.isNotBlank(name)) {
            wrapper.like(ModelEntity::getName, name);
        }
        if (StringUtils.isNotBlank(createUser)) {
            wrapper.eq(ModelEntity::getUserId, UUID.fromString(createUser));
        }
        if (StringUtils.isNotBlank(permissionType)) {
            wrapper.eq(ModelEntity::getPermissionType, permissionType);
        }
        if (StringUtils.isNotBlank(modelType)) {
            wrapper.eq(ModelEntity::getModelType, modelType);
        }
        List<ModelEntity> modelEntities = baseMapper.selectList(wrapper);
        if (CollectionUtils.isNotEmpty(modelEntities)) {
            List<ModelVO> models = BeanUtil.copyList(modelEntities, ModelVO.class);
            models.forEach(model -> model.setUsername(userMap.get(model.getId())));
            return models;
        }
        return Collections.emptyList();
    }

    public <T> T getModelById(UUID modelId) {
        ModelEntity model = this.getById(modelId);
        SystemSettingEntity systemSetting = systemSettingService.lambdaQuery().eq(SystemSettingEntity::getType, 1).one();
        return ModelManage.getModel(model, systemSetting.getMeta().getString("value"));
    }


}
