package com.tarzan.maxkb4j.module.model.provider.impl;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.DisabledImageModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;

public  class BaseTextToImage {

    private final ImageModel imageModel;

    public BaseTextToImage() {
        this.imageModel = new DisabledImageModel();
    }

    public BaseTextToImage(ImageModel imageModel) {
        this.imageModel = imageModel;
    }

    public Response<Image> generateImage(String prompt, String negativePrompt){
        prompt=prompt+negativePrompt;
        return imageModel.generate(prompt);
    }
}
