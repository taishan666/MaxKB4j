package com.tarzan.maxkb4j.module.model.info.service;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.model.info.entity.ModelEntity;
import com.tarzan.maxkb4j.module.model.info.mapper.ModelMapper;
import com.tarzan.maxkb4j.module.model.info.vo.ModelVO;
import com.tarzan.maxkb4j.module.model.provider.ModelFactory;
import com.tarzan.maxkb4j.module.system.permission.service.UserResourcePermissionService;
import com.tarzan.maxkb4j.module.system.user.service.UserService;
import com.tarzan.maxkb4j.util.BeanUtil;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author tarzan
 * @date 2024-12-25 12:22:22
 */
@Service
@AllArgsConstructor
public class ModelService extends ServiceImpl<ModelMapper, ModelEntity> {

    private final UserService userService;
    private final ModelBaseService modelBaseService;
    private final UserResourcePermissionService userResourcePermissionService;

    public List<ModelEntity> getUserIdAndType(String userId, String modelType) {
        modelType = StringUtils.isBlank(modelType) ? "LLM" : modelType;
        return this.list(Wrappers.<ModelEntity>lambdaQuery().eq(ModelEntity::getUserId, userId).eq(ModelEntity::getModelType, modelType));
    }

    public List<ModelEntity> models(String modelType) {
        return modelBaseService.models(modelType);
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
        List<String> targetIds =userResourcePermissionService.getTargetIds("MODEL",loginId);
        if (org.springframework.util.CollectionUtils.isEmpty(targetIds)){
            wrapper.eq(ModelEntity::getUserId, loginId);
        }else {
            wrapper.and(
                    w -> w.eq(ModelEntity::getUserId, loginId)
                            .or()
                            .in(ModelEntity::getId, targetIds));
        }
        wrapper.orderByDesc(ModelEntity::getCreateTime);
        List<ModelEntity> modelEntities = baseMapper.selectList(wrapper);
        if (CollectionUtils.isNotEmpty(modelEntities)) {
            List<ModelVO> models = BeanUtil.copyList(modelEntities, ModelVO.class);
            models.forEach(model -> model.setNickname(userMap.get(model.getUserId())));
            return models;
        }
        return Collections.emptyList();
    }


    public <T> T getModelById(String modelId) {
        return modelBaseService.getModelById(modelId);
    }

    public <T> T getModelById(String modelId,JSONObject modelParams) {
        ModelEntity model = modelBaseService.getModelInfoById(modelId);
        return ModelFactory.build(model,modelParams);
    }


    public Boolean createModel(ModelEntity model) {
        return modelBaseService.createModel(model);
    }

    public ModelEntity updateModel(String id, ModelEntity model) {
        return modelBaseService.updateModel(id, model);
    }
}
