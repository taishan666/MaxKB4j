package com.tarzan.maxkb4j.module.model.provider;

import com.alibaba.fastjson.JSONObject;

import java.util.Collections;
import java.util.List;

public abstract class BaseModelCredential {

    protected abstract JSONObject encryptionDict(JSONObject modelInfo);

    public abstract List<JSONObject> getModelParamsSettingForm(String modelName);

    public String encryption(String text) {
        return "";
    }

    public List<JSONObject> toForm() {
        return Collections.emptyList();
    }
}
