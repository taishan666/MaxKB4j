package com.tarzan.maxkb4j.module.model.info.service;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.common.util.StringUtil;
import com.tarzan.maxkb4j.module.model.info.entity.ModelEntity;
import com.tarzan.maxkb4j.module.model.provider.service.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author tarzan
 * @date 2024-12-25 12:22:22
 */
@Service
@AllArgsConstructor
public class ModelFactory {


    private final ModelService modelService;


    public <T> T build(String modelId) {
        return build(modelId, new JSONObject());
    }

    public <T> T build(String modelId, JSONObject modelParams) {
        if (StringUtil.isBlank(modelId)) {
            return null;
        }
        ModelEntity model = modelService.getCacheModelById(modelId);
        if (model == null) {
            return null;
        }
        IModelProvider modelProvider = ModelProviderEnum.get(model.getProvider());
        return modelProvider.build(model.getModelName(), model.getModelType(), model.getCredential(), modelParams);
    }


}
