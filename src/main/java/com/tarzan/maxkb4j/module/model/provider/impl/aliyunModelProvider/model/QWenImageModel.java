package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import dev.langchain4j.community.model.dashscope.WanxImageModel;
import dev.langchain4j.community.model.dashscope.WanxImageSize;
import dev.langchain4j.model.image.ImageModel;

public class QWenImageModel implements BaseModel<ImageModel> {

    @Override
    public WanxImageModel build(String modelName, ModelCredential credential, JSONObject params) {
        return  WanxImageModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .size(params==null?WanxImageSize.SIZE_1024_1024:WanxImageSize.of(params.getString("size")))
                .promptExtend(params==null&&params.getBoolean("prompt_extend"))
                .negativePrompt(params==null?null:params.getString("negative_prompt"))
                .build();
    }




}
