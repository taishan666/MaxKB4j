package com.maxkb4j.model.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maxkb4j.common.constant.RoleType;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.common.mp.entity.ModelCredential;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.common.util.DataMaskUtil;
import com.maxkb4j.common.util.StpKit;
import com.maxkb4j.model.entity.ModelEntity;
import com.maxkb4j.model.mapper.ModelMapper;
import com.maxkb4j.model.vo.ModelVO;
import com.maxkb4j.system.constant.AuthTargetType;
import com.maxkb4j.user.service.IUserResourcePermissionService;
import com.maxkb4j.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author tarzan
 * @date 2024-12-25 12:22:22
 */
@Service
@RequiredArgsConstructor
public class ModelService extends ServiceImpl<ModelMapper, ModelEntity> {

    private final IUserService userService;
    private final IUserResourcePermissionService userResourcePermissionService;

    private static final Cache<String, ModelEntity> MODEL_CACHE = Caffeine.newBuilder()
            .initialCapacity(100)
            // 超出最大容量时淘汰
            .maximumSize(10000)
            //设置写缓存后n秒钟过期
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build();

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
        String loginId = StpKit.ADMIN.getLoginIdAsString();
        Set<String> role = userService.getRoleById(loginId);
        if (!CollectionUtils.isEmpty(role)) {
            if (role.contains(RoleType.USER)) {
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
        String userId = StpKit.ADMIN.getLoginIdAsString();
        long count = this.lambdaQuery().eq(ModelEntity::getName, model.getName()).eq(ModelEntity::getUserId, userId).count();
        if (count > 0) {
            throw new ApiException("模型名称已存在");
        }
        if (model.getModelParamsForm() == null){
            model.setModelParamsForm(new JSONArray());
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
        MODEL_CACHE.put(id, model);
        this.updateById(model);
        return model;
    }

    @Transactional
    public Boolean removeModelById(String id) {
        userResourcePermissionService.remove(AuthTargetType.MODEL, id);
        return this.removeById(id);
    }

    public ModelEntity getInfo(String id) {
        ModelEntity model = this.getById(id);
        if (model != null){
            String userId = StpKit.ADMIN.getLoginIdAsString();
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
            String userId = StpKit.ADMIN.getLoginIdAsString();
            if (model.getUserId().equals(userId)){
                return model.getCredential();
            }
        }
        return null;
    }

    public ModelEntity getModelById(String id) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
       return MODEL_CACHE.get(id,modelId->this.lambdaQuery()
               .select(ModelEntity::getProvider, ModelEntity::getModelType, ModelEntity::getModelName, ModelEntity::getCredential)
               .eq(ModelEntity::getId, modelId)
               .one());
    }
}
