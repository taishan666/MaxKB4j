package com.tarzan.maxkb4j.module.model.provider;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInputVO;

import java.util.List;

public abstract class BaseModelCredential {

    protected abstract JSONObject encryptionDict(JSONObject modelInfo);

    public abstract List<ModelInputVO> getModelParamsSettingForm();

    public String encryption(String text) {
        return "";
    }

    public abstract List<ModelInputVO> toForm();
}
