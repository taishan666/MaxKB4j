package com.tarzan.maxkb4j.module.model.info.service;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.model.info.entity.ModelEntity;
import com.tarzan.maxkb4j.module.model.info.mapper.ModelMapper;
import com.tarzan.maxkb4j.module.model.provider.ModelFactory;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 12:22:22
 */
@Service
@AllArgsConstructor
public class ModelBaseService extends ServiceImpl<ModelMapper, ModelEntity> {


    public List<ModelEntity> models(String modelType) {
        modelType = StringUtils.isBlank(modelType) ? "LLM" : modelType;
        return baseMapper.selectList(Wrappers.<ModelEntity>lambdaQuery().eq(ModelEntity::getModelType, modelType));
    }


    @Cacheable(cacheNames = "model_info", key = "#modelId")
    public ModelEntity getModelInfoById(String modelId) {
        return this.getById(modelId);
    }


    @Cacheable(cacheNames = "model", key = "#modelId")
    public <T> T getModelById(String modelId) {
        ModelEntity model = getModelInfoById(modelId);
        return ModelFactory.build(model);
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
