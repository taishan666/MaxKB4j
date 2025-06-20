package com.tarzan.maxkb4j.module.model.provider;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelEntity;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ModelFactory {

    public static <T> T build(ModelEntity model) {
        return ModelProviderEnum.get(model.getProvider()).build(model.getModelName(), model.getModelType(), model.getCredential());
    }

    public static <T> T build(ModelEntity model, JSONObject params) {
        return ModelProviderEnum.get(model.getProvider()).build(model.getModelName(), model.getModelType(), model.getCredential(),params);
    }

}
