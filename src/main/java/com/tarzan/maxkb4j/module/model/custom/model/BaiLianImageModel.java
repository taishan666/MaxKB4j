package com.tarzan.maxkb4j.module.model.custom.model;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;

import java.util.List;

public class BaiLianImageModel implements ImageModel {
    @Override
    public Response<Image> generate(String prompt) {
        return null;
    }

    @Override
    public Response<List<Image>> generate(String prompt, int n) {
        return ImageModel.super.generate(prompt, n);
    }

    @Override
    public Response<Image> edit(Image image, String prompt) {
        return ImageModel.super.edit(image, prompt);
    }

    @Override
    public Response<Image> edit(Image image, Image mask, String prompt) {
        return ImageModel.super.edit(image, mask, prompt);
    }
}
