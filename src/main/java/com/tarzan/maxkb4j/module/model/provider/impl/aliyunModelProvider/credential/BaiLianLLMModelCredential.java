package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.credential;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.provider.BaseModelCredential;

import java.util.List;

public class BaiLianLLMModelCredential extends BaseModelCredential {

    @Override
    protected JSONObject encryptionDict(JSONObject modelInfo) {
        return null;
    }

    @Override
    public List<JSONObject> getModelParamsSettingForm(String modelName) {
        return List.of();
    }
}
