package com.tarzan.maxkb4j.module.model.info.service;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.model.info.entity.ModelEntity;
import com.tarzan.maxkb4j.module.model.info.mapper.ModelMapper;
import com.tarzan.maxkb4j.module.model.provider.ModelManage;
import com.tarzan.maxkb4j.module.model.info.vo.ModelVO;
import com.tarzan.maxkb4j.module.system.user.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.service.UserService;
import com.tarzan.maxkb4j.util.BeanUtil;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author tarzan
 * @date 2024-12-25 12:22:22
 */
@Service
@AllArgsConstructor
public class ModelService extends ServiceImpl<ModelMapper, ModelEntity> {

    private final UserService userService;

    public List<ModelEntity> getUserIdAndType(String userId, String modelType) {
        modelType = StringUtils.isBlank(modelType) ? "LLM" : modelType;
        return this.list(Wrappers.<ModelEntity>lambdaQuery().eq(ModelEntity::getUserId, userId).eq(ModelEntity::getModelType, modelType));
    }

    public List<ModelEntity> models(String modelType) {
        modelType = StringUtils.isBlank(modelType) ? "LLM" : modelType;
        return baseMapper.selectList(Wrappers.<ModelEntity>lambdaQuery().eq(ModelEntity::getModelType, modelType));
    }

    public List<ModelVO> models(String name, String createUser, String permissionType, String modelType) {
        List<UserEntity> users = userService.lambdaQuery().list();
        Map<String, String> userMap = users.stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getUsername));
        LambdaQueryWrapper<ModelEntity> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.isNotBlank(name)) {
            wrapper.like(ModelEntity::getName, name);
        }
        if (StringUtils.isNotBlank(createUser)) {
            wrapper.eq(ModelEntity::getUserId, createUser);
        }
        if (StringUtils.isNotBlank(permissionType)) {
            wrapper.eq(ModelEntity::getPermissionType, permissionType);
        }
        if (StringUtils.isNotBlank(modelType)) {
            wrapper.eq(ModelEntity::getModelType, modelType);
        }
        wrapper.eq(ModelEntity::getUserId, StpUtil.getLoginIdAsString());
        wrapper.or().eq(ModelEntity::getPermissionType, "PUBLIC");
        List<ModelEntity> modelEntities = baseMapper.selectList(wrapper);
        if (CollectionUtils.isNotEmpty(modelEntities)) {
            List<ModelVO> models = BeanUtil.copyList(modelEntities, ModelVO.class);
            models.forEach(model -> model.setUsername(userMap.get(model.getUserId())));
            return models;
        }
        return Collections.emptyList();
    }

    @Cacheable(cacheNames = "model", key = "#modelId")
    public <T> T getModelById(String modelId) {
        ModelEntity model = this.getById(modelId);
        return ModelManage.getModel(model);
    }


    //todo 缓存处理
    public <T> T getModelById(String modelId, JSONObject modelParams) {
        ModelEntity model = this.getById(modelId);
        return ModelManage.getModel(model);
    }


    public Boolean createModel(ModelEntity model) {
        String userId = StpUtil.getLoginIdAsString();
        long count=this.lambdaQuery().eq(ModelEntity::getName, model.getName()).eq(ModelEntity::getUserId, userId).count();
        if(count>0){
            return false;
        }
        model.setUserId(userId);
        model.setMeta(new JSONObject());
        model.setStatus("SUCCESS");
        return save(model);
    }

    public ModelEntity updateModel(String id, ModelEntity model) {
        model.setId(id);
        this.updateById(model);
        return model;
    }
}
