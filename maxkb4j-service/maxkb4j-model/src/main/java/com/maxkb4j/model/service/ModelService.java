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
import com.maxkb4j.model.enums.ModelStatus;
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
import java.util.function.Function;

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

    public List<ModelVO> modelList(String name, String modelName, String modelType, String provider) {
        LambdaQueryWrapper<ModelEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.select(ModelEntity::getId,
                ModelEntity::getName,
                ModelEntity::getProvider,
                ModelEntity::getStatus
        );
        applyCommonFilters(wrapper, name, modelType, provider);
        if (StringUtils.isNotBlank(modelName)) {
            wrapper.eq(ModelEntity::getModelName, modelName);
        }
        applyDataPermission(wrapper);
        wrapper.orderByDesc(ModelEntity::getCreateTime);
        List<ModelEntity> modelEntities = this.list(wrapper);
        if (CollectionUtils.isNotEmpty(modelEntities)) {
            return BeanUtil.copyList(modelEntities, ModelVO.class);
        }
        return Collections.emptyList();
    }

    public List<ModelVO> models(String name, String createUserId, String modelType, String provider) {
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
        applyCommonFilters(wrapper, name, modelType, provider);
        if (StringUtils.isNotBlank(createUserId)) {
            wrapper.eq(ModelEntity::getUserId, createUserId);
        }
        applyDataPermission(wrapper);
        wrapper.orderByDesc(ModelEntity::getCreateTime);
        List<ModelEntity> modelEntities = this.list(wrapper);
        if (CollectionUtils.isEmpty(modelEntities)) {
            return Collections.emptyList();
        }
        Map<String, String> userMap = userService.getNicknameMap();
        List<ModelVO> models = BeanUtil.copyList(modelEntities, ModelVO.class);
        models.forEach(model -> model.setNickname(userMap.get(model.getUserId())));
        return models;
    }

    @Transactional
    public boolean createModel(ModelEntity model) {
        String userId = StpKit.ADMIN.getLoginIdAsString();
        long count = this.lambdaQuery().eq(ModelEntity::getName, model.getName()).eq(ModelEntity::getUserId, userId).count();
        if (count > 0) {
            throw new ApiException("model.name.exists");
        }
        if (model.getModelParamsForm() == null){
            model.setModelParamsForm(new JSONArray());
        }
        model.setUserId(userId);
        model.setMeta(new JSONObject());
        model.setStatus(ModelStatus.SUCCESS.getKey());
        save(model);
        return userResourcePermissionService.ownerSave(AuthTargetType.MODEL, model.getId(), model.getUserId());
    }

    public ModelEntity updateModel(String id, ModelEntity model) {
        model.setId(id);
        ModelCredential credential = getModelCredential(id);
        String maskApiKey = DataMaskUtil.maskApiKey(credential.getApiKey());
        if (maskApiKey != null && maskApiKey.equals(model.getCredential().getApiKey())) {
            credential.setBaseUrl(model.getCredential().getBaseUrl());
            model.setCredential(credential);
        }
        this.updateById(model);
        evictCache(id);
        return model;
    }

    @Transactional
    public Boolean removeModelById(String id) {
        userResourcePermissionService.remove(AuthTargetType.MODEL, id);
        boolean removed = this.removeById(id);
        evictCache(id);
        return removed;
    }

    public ModelEntity getInfo(String id) {
        return getOwnedModel(id, model -> {
            ModelCredential credential = model.getCredential();
            credential.setApiKey(DataMaskUtil.maskApiKey(credential.getApiKey()));
            return model;
        });
    }

    public ModelCredential getModelCredential(String id) {
        return getOwnedModel(id, ModelEntity::getCredential);
    }

    public ModelEntity getModelById(String id) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        return MODEL_CACHE.get(id, modelId -> this.lambdaQuery()
                .select(ModelEntity::getProvider, ModelEntity::getModelType, ModelEntity::getModelName, ModelEntity::getCredential)
                .eq(ModelEntity::getId, modelId)
                .one());
    }

    /**
     * 应用通用筛选条件：名称模糊、模型类型、供应商。
     */
    private void applyCommonFilters(LambdaQueryWrapper<ModelEntity> wrapper, String name, String modelType, String provider) {
        if (StringUtils.isNotBlank(name)) {
            wrapper.like(ModelEntity::getName, name);
        }
        if (StringUtils.isNotBlank(modelType)) {
            wrapper.eq(ModelEntity::getModelType, modelType);
        }
        if (StringUtils.isNotBlank(provider)) {
            wrapper.eq(ModelEntity::getProvider, provider);
        }
    }

    /**
     * 应用数据权限：
     * - 普通用户：仅可见已授权的模型，无授权则强制空结果
     * - 无任何角色：强制空结果
     * - 其他角色（如管理员）：不附加限制
     */
    private void applyDataPermission(LambdaQueryWrapper<ModelEntity> wrapper) {
        String loginId = StpKit.ADMIN.getLoginIdAsString();
        Set<String> roles = userService.getRoleById(loginId);
        if (CollectionUtils.isEmpty(roles)) {
            wrapper.last(" limit 0");
            return;
        }
        if (!roles.contains(RoleType.USER)) {
            return;
        }
        List<String> targetIds = userResourcePermissionService.getTargetIds(AuthTargetType.MODEL, loginId);
        if (org.springframework.util.CollectionUtils.isEmpty(targetIds)) {
            wrapper.last(" limit 0");
        } else {
            wrapper.in(ModelEntity::getId, targetIds);
        }
    }

    /**
     * 以"当前登录者必须是模型拥有者"为前提取出模型，再由 mapper 决定返回内容。
     * 非拥有者或模型不存在时返回 null。
     */
    private <T> T getOwnedModel(String id, Function<ModelEntity, T> mapper) {
        ModelEntity model = this.getById(id);
        if (model == null) {
            return null;
        }
        String userId = StpKit.ADMIN.getLoginIdAsString();
        if (!model.getUserId().equals(userId)) {
            return null;
        }
        return mapper.apply(model);
    }

    private void evictCache(String id) {
        MODEL_CACHE.invalidate(id);
    }

}
