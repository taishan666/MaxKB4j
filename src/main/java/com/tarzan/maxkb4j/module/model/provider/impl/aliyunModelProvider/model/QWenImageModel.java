package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.model;

import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import dev.langchain4j.model.image.ImageModel;

public class QWenImageModel implements BaseModel<ImageModel> {

    @Override
    public WanXImageModel build(String modelName, ModelCredential credential, JSONObject params) {
        ImageSynthesisParam.ImageSynthesisParamBuilder<?, ?> paramBuilder=ImageSynthesisParam.builder();
        assert params != null;
        paramBuilder.apiKey(credential.getApiKey())
                .model(modelName)
                .size((String) params.getOrDefault("size", "1024*1024"))
                .function((String) params.getOrDefault("function", "stylization_all"))
               // .promptExtend(params == null || params.getBoolean("prompt_extend"))
                .negativePrompt(params.getString("negative_prompt"));
           return new WanXImageModel(paramBuilder);
       // WanxImageModel
/*        return  WanxImageModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .size(params==null?WanxImageSize.SIZE_1024_1024:WanxImageSize.of(params.getString("size")))
                .promptExtend(params==null&&params.getBoolean("prompt_extend"))
                .negativePrompt(params.getString("negative_prompt"))
                .build();*/
    }




}
