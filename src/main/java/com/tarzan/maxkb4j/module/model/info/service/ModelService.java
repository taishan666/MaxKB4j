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
import com.tarzan.maxkb4j.module.system.permission.constant.AuthTargetType;
import com.tarzan.maxkb4j.module.system.permission.service.UserResourcePermissionService;
import com.tarzan.maxkb4j.module.system.user.domain.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.service.UserService;
import com.tarzan.maxkb4j.common.util.BeanUtil;
import com.tarzan.maxkb4j.common.util.StringUtil;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
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
    private final ModelBaseService modelBaseService;
    private final UserResourcePermissionService userResourcePermissionService;

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
        UserEntity user = userService.getById(loginId);
        if (Objects.nonNull(user)){
            if (!org.springframework.util.CollectionUtils.isEmpty(user.getRole())){
                if (user.getRole().contains("USER")){
                    List<String> targetIds =userResourcePermissionService.getTargetIds("MODEL",loginId);
                    if (!org.springframework.util.CollectionUtils.isEmpty(targetIds)){
                        wrapper.in(ModelEntity::getId, targetIds);
                    }else {
                        wrapper.last(" limit 0");
                    }
                }
            }else {
                wrapper.last(" limit 0");
            }
        }else{
            wrapper.last(" limit 0");
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
        return getModelById(modelId,new JSONObject());
    }

    public <T> T getModelById(String modelId,JSONObject modelParams) {
        if (StringUtil.isBlank(modelId)){
            return null;
        }
        ModelEntity model = modelBaseService.getModelInfoById(modelId);
        if (model == null){
            return null;
        }
        return ModelFactory.build(model,modelParams);
    }


    @Transactional
    public Boolean createModel(ModelEntity model) {
         modelBaseService.createModel(model);
        return userResourcePermissionService.ownerSave(AuthTargetType.MODEL, model.getId(),model.getUserId());
    }

    public ModelEntity updateModel(String id, ModelEntity model) {
        return modelBaseService.updateModel(id, model);
    }

    @Transactional
    public Boolean removeModelById(String id) {
        userResourcePermissionService.remove(AuthTargetType.APPLICATION, id);
        return modelBaseService.removeById(id);
    }
}
