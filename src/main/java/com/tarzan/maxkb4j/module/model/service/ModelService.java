package com.tarzan.maxkb4j.module.model.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import com.tarzan.maxkb4j.module.model.mapper.ModelMapper;
import com.tarzan.maxkb4j.module.model.entity.ModelEntity;

import java.util.List;
import java.util.UUID;

/**
 * @author tarzan
 * @date 2024-12-25 12:22:22
 */
@Service
public class ModelService extends ServiceImpl<ModelMapper, ModelEntity>{

    public List<ModelEntity> getUserIdAndType(UUID userId, String modelType) {
        modelType= StringUtils.isBlank(modelType)?"LLM":modelType;
        return this.list(Wrappers.<ModelEntity>lambdaQuery().eq(ModelEntity::getUserId, userId).eq(ModelEntity::getModelType, modelType));
    }

    public List<ModelEntity> models(String modelType) {
        modelType= StringUtils.isBlank(modelType)?"LLM":modelType;
        return baseMapper.selectList(Wrappers.<ModelEntity>lambdaQuery().eq(ModelEntity::getModelType,modelType));
    }

}
