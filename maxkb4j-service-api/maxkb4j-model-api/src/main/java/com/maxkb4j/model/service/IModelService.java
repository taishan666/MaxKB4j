package com.maxkb4j.model.service;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.model.entity.ModelEntity;
import com.maxkb4j.model.vo.ModelVO;

import java.util.List;

/**
 * Model service interface
 */
public interface IModelService extends IService<ModelEntity> {

    List<ModelVO> modelList(String name, String modelName, String modelType, String provider);

    List<ModelVO> models(String name, String createUserId, String modelType, String provider);

    boolean createModel(ModelEntity model);

    ModelEntity updateModel(String id, ModelEntity model);

    Boolean removeModelById(String id);

    ModelEntity getInfo(String id);

    ModelEntity getModelById(String id);

    void updateModelParamsForm(String id, JSONArray paramsForm);
}
