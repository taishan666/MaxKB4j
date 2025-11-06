package com.tarzan.maxkb4j.module.model.info.service;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.common.util.BeanUtil;
import com.tarzan.maxkb4j.common.util.DataMaskUtil;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.info.entity.ModelEntity;
import com.tarzan.maxkb4j.module.model.info.mapper.ModelMapper;
import com.tarzan.maxkb4j.module.model.info.vo.ModelVO;
import com.tarzan.maxkb4j.module.system.permission.constant.AuthTargetType;
import com.tarzan.maxkb4j.module.system.permission.service.UserResourcePermissionService;
import com.tarzan.maxkb4j.module.system.user.domain.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.service.UserService;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author tarzan
 * @date 2024-12-25 12:22:22
 */
@Service
@AllArgsConstructor
public class ModelService extends ServiceImpl<ModelMapper, ModelEntity> {

    private final UserService userService;
    private final UserResourcePermissionService userResourcePermissionService;


    @Cacheable(cacheNames = "model_info", key = "#modelId")
    public ModelEntity getCacheModelById(String modelId) {
        return this.getById(modelId);
    }


    public List<ModelVO> models(String name, String createUser, String modelType, String provider) {
        Map<String, String> userMap = userService.getNicknameMap();
        LambdaQueryWrapper<ModelEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.select(ModelEntity::getId,
                ModelEntity::getName,
                ModelEntity::getModelName,
                ModelEntity::getModelType,
                ModelEntity::getProvider,
                ModelEntity::getUserId,
                ModelEntity::getStatus,
                ModelEntity::getCreateTime,
                ModelEntity::getUpdateTime
        );
        if (StringUtils.isNotBlank(name)) {
            wrapper.like(ModelEntity::getName, name);
        }
        if (StringUtils.isNotBlank(createUser)) {
            wrapper.eq(ModelEntity::getUserId, createUser);
        }
        if (StringUtils.isNotBlank(modelType)) {
            wrapper.eq(ModelEntity::getModelType, modelType);
        }
        if (StringUtils.isNotBlank(provider)) {
            wrapper.eq(ModelEntity::getProvider, provider);
        }
        String loginId = StpUtil.getLoginIdAsString();
        UserEntity user = userService.getById(loginId);
        if (Objects.nonNull(user)) {
            if (!org.springframework.util.CollectionUtils.isEmpty(user.getRole())) {
                if (user.getRole().contains("USER")) {
                    List<String> targetIds = userResourcePermissionService.getTargetIds(AuthTargetType.MODEL, loginId);
                    if (!org.springframework.util.CollectionUtils.isEmpty(targetIds)) {
                        wrapper.in(ModelEntity::getId, targetIds);
                    } else {
                        wrapper.last(" limit 0");
                    }
                }
            } else {
                wrapper.last(" limit 0");
            }
        } else {
            wrapper.last(" limit 0");
        }
        wrapper.orderByDesc(ModelEntity::getCreateTime);
        List<ModelEntity> modelEntities = this.list(wrapper);
        if (CollectionUtils.isNotEmpty(modelEntities)) {
            List<ModelVO> models = BeanUtil.copyList(modelEntities, ModelVO.class);
            models.forEach(model -> model.setNickname(userMap.get(model.getUserId())));
            return models;
        }
        return Collections.emptyList();
    }

    @Transactional
    public boolean createModel(ModelEntity model) {
        String userId = StpUtil.getLoginIdAsString();
        long count = this.lambdaQuery().eq(ModelEntity::getName, model.getName()).eq(ModelEntity::getUserId, userId).count();
        if (count > 0) {
            return false;
        }
        model.setUserId(userId);
        model.setMeta(new JSONObject());
        model.setStatus("SUCCESS");
        save(model);
        return userResourcePermissionService.ownerSave(AuthTargetType.MODEL, model.getId(), model.getUserId());
    }


    public ModelEntity updateModel(String id, ModelEntity model) {
        model.setId(id);
        ModelCredential credential=getModelCredential(id);
        String maskApiKey= DataMaskUtil.maskApiKey(credential.getApiKey());
        if (maskApiKey != null&& maskApiKey.equals(model.getCredential().getApiKey())){
            credential.setBaseUrl(model.getCredential().getBaseUrl());
            model.setCredential(credential);
        }
        this.updateById(model);
        return model;
    }

    @Transactional
    public Boolean removeModelById(String id) {
        userResourcePermissionService.remove(AuthTargetType.APPLICATION, id);
        return this.removeById(id);
    }

    public ModelEntity getInfo(String id) {
        ModelEntity model = this.getById(id);
        if (model != null){
            String userId = StpUtil.getLoginIdAsString();
            if (model.getUserId().equals(userId)){
                ModelCredential credential=model.getCredential();
                String apiKey=credential.getApiKey();
                credential.setApiKey(DataMaskUtil.maskApiKey(apiKey));
            }
        }
        return model;
    }

    public ModelCredential getModelCredential(String id) {
        ModelEntity model = this.getById(id);
        if (model != null){
            String userId = StpUtil.getLoginIdAsString();
            if (model.getUserId().equals(userId)){
                return model.getCredential();
            }
        }
        return null;
    }
}
