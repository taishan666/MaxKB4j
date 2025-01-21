package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseTextToImage;
import dev.langchain4j.model.dashscope.WanxImageModel;

public class QWenTextToImageModel extends BaseTextToImage implements BaseModel {

    @Override
    public <T> T newInstance(String modelName, JSONObject modelCredential) {
        WanxImageModel model = WanxImageModel.builder()
                .apiKey(modelCredential.getString("api_key"))
                .modelName(modelName).build();
        return (T) new BaseTextToImage(model);
    }


}
