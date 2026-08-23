package com.maxkb4j.model.custom.model;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.entity.ModelCredential;
import dev.langchain4j.community.model.dashscope.WanxImageModel;
import dev.langchain4j.community.model.dashscope.WanxImageSize;
import dev.langchain4j.community.model.dashscope.WanxImageStyle;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;

import java.util.List;
import static com.maxkb4j.model.consts.ModelConstants.*;

public class BaiLianImageModel implements ImageModel {

    private final ModelCredential credential;
    private final JSONObject params;
    private final ImageModel instance;

    public BaiLianImageModel(String modelName, ModelCredential credential, JSONObject params) {
        this.credential = credential;
        this.params = params;
        this.instance = buildInstance(modelName);
    }

    private ImageModel buildInstance(String modelName) {
        if (modelName.startsWith(ModelName.WANX_PREFIX)||modelName.startsWith(ModelName.WAN2_PREFIX)){
            return WanxImageModel.builder()
                    .modelName(modelName)
                    .apiKey(credential.getApiKey())
                    .size(WanxImageSize.of(params.getString(ParamKey.SIZE)))
                    .style(WanxImageStyle.of(params.getString(ParamKey.STYLE)))
                    .negativePrompt(params.getString(ParamKey.NEGATIVE_PROMPT))
                    .promptExtend(params.getBoolean(ParamKey.PROMPT_EXTEND))
                    .watermark(params.getBoolean(ParamKey.WATERMARK))
                    .seed(params.getInteger(ParamKey.SEED))
                    .build();
        }else {
            return new QwenImageModel(modelName, credential, params);
        }

    }

    @Override
    public Response<Image> generate(String prompt) {
        return instance.generate(prompt);
    }

    @Override
    public Response<List<Image>> generate(String prompt, int n) {
        return instance.generate(prompt, n);
    }

    @Override
    public Response<Image> edit(Image image, String prompt) {
        return instance.edit(image, prompt);
    }

    @Override
    public Response<Image> edit(Image image, Image mask, String prompt) {
        return instance.edit(image, mask, prompt);
    }
}
