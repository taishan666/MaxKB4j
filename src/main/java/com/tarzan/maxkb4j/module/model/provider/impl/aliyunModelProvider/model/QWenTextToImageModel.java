package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.model;

import com.tarzan.maxkb4j.module.model.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseTextToImage;
import dev.langchain4j.community.model.dashscope.WanxImageModel;

public class QWenTextToImageModel extends BaseTextToImage implements BaseModel {

    @Override
    public <T> T newInstance(String modelName, ModelCredential credential) {
        WanxImageModel model = WanxImageModel.builder()
                .baseUrl(credential.getApiBase())
                .apiKey(credential.getApiKey())
                .modelName(modelName).build();
        return (T) new BaseTextToImage(model);
    }


}
