package com.tarzan.maxkb4j.module.model.provider;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.entity.ModelEntity;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.util.RSAUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ModelManage {

    public static <T> T getModel(ModelEntity model,String encryptPrivateKey) {
        return getProvider(model.getProvider()).getModel(model.getModelName(), model.getModelType(), getCredential(model.getCredential(),encryptPrivateKey));
    }


    public static IModelProvider getProvider(String provider) {
        return ModelProviderEnum.get(provider);
    }

    public static JSONObject getCredential(String credential,String encryptPrivateKey) {
        try {
            String text = RSAUtil.rsaLongDecrypt(credential, encryptPrivateKey);
            return JSONObject.parseObject(text);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return new JSONObject();
    }

}
