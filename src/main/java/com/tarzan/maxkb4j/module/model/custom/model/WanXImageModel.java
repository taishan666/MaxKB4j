package com.tarzan.maxkb4j.module.model.custom.model;

import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.OSSUtils;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.internal.Utils;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;
import lombok.NoArgsConstructor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@NoArgsConstructor(force = true)
public class WanXImageModel implements ImageModel{

    private ImageSynthesisParam.ImageSynthesisParamBuilder<?, ?> paramBuilder;

    public WanXImageModel(String modelName, ModelCredential credential, JSONObject params) {
        assert params != null;
        paramBuilder= ImageSynthesisParam.builder().apiKey(credential.getApiKey())
                .model(modelName)
                .size((String) params.getOrDefault("size", "1024*1024"))
                .function(params.getString("function"))
                .promptExtend(params.getBoolean("prompt_extend"))
                .seed(params.getInteger("seed"))
                .negativePrompt(params.getString("negative_prompt"))
                .watermark(params.getBoolean("watermark"));
    }


    @Override
    public Response<Image> generate(String prompt) {
        ImageSynthesis imageSynthesis = new ImageSynthesis();
        paramBuilder.prompt(prompt);
        try {
            ImageSynthesisResult result = imageSynthesis.call(paramBuilder.build());
            return Response.from(imagesFrom(result).get(0));
        } catch (NoApiKeyException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public Response<List<Image>> generate(String prompt, int n) {
        paramBuilder.prompt(prompt);
        paramBuilder.n(n);
        ImageSynthesis imageSynthesis = new ImageSynthesis();
        try {
            ImageSynthesisResult result = imageSynthesis.call(paramBuilder.build());
            return Response.from(imagesFrom(result));
        } catch (NoApiKeyException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public Response<Image> edit(Image image, String prompt) {
        ImageSynthesisParam param=paramBuilder.build();
        String imageUrl = imageUrl(image, param.getModel(), param.getApiKey());
        if (imageUrl.startsWith("oss://")) {
            Map<String, Object> headers = new HashMap<>();
            headers.put("X-DashScope-OssResourceResolve", "enable");
            param.setHeaders(headers);
        }
        param.setBaseImageUrl(imageUrl);
        ImageSynthesis imageSynthesis = new ImageSynthesis("image2image");
        try {
            ImageSynthesisResult result = imageSynthesis.call(param);
            return Response.from(imagesFrom(result).get(0));
        } catch (NoApiKeyException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public Response<Image> edit(Image image, Image mask, String prompt) {
        ImageSynthesisParam param=paramBuilder.build();
        String imageUrl = imageUrl(image, param.getModel(), param.getApiKey());
        String maskUrl = imageUrl(mask, param.getModel(), param.getApiKey());
        Map<String, Object> headers = new HashMap<>();
        if (imageUrl.startsWith("oss://") || maskUrl.startsWith("oss://")) {
            headers.put("X-DashScope-OssResourceResolve", "enable");
        }
        param.setBaseImageUrl(imageUrl);
        param.setMaskImageUrl(maskUrl);
        param.setHeaders(headers);
        ImageSynthesis imageSynthesis = new ImageSynthesis();
        try {
            ImageSynthesisResult result = imageSynthesis.call(param);
            return Response.from(imagesFrom(result).get(0));
        } catch (NoApiKeyException e) {
            throw new IllegalArgumentException(e);
        }
    }

    static List<Image> imagesFrom(ImageSynthesisResult result) {
        return result.getOutput().getResults().stream().map(resultMap -> Image.builder().url(resultMap.get("url")).revisedPrompt(resultMap.get("actual_prompt")).build()).toList();
    }

    static String imageUrl(Image image, String model, String apiKey) {
        String imageUrl;
        if (image.url() != null) {
            imageUrl = image.url().toString();
        } else {
            if (!Utils.isNotNullOrBlank(image.base64Data())) {
                throw new IllegalArgumentException("Failed to get image url from " + image);
            }
            String filePath = saveDataAsTemporaryFile(image.base64Data(), image.mimeType());
            try {
                imageUrl = OSSUtils.upload(model, filePath, apiKey);
            } catch (NoApiKeyException e) {
                throw new IllegalArgumentException(e);
            }
        }

        return imageUrl;
    }


    static String saveDataAsTemporaryFile(String base64Data, String mimeType) {
        String tmpDir = System.getProperty("java.io.tmpdir", "/tmp");
        String tmpFileName = UUID.randomUUID().toString();
        if (Utils.isNotNullOrBlank(mimeType)) {
            int lastSlashIndex = mimeType.lastIndexOf("/");
            if (lastSlashIndex >= 0 && lastSlashIndex < mimeType.length() - 1) {
                String fileSuffix = mimeType.substring(lastSlashIndex + 1);
                tmpFileName = tmpFileName + "." + fileSuffix;
            }
        }

        Path tmpFilePath = Paths.get(tmpDir, tmpFileName);
        byte[] data = Base64.getDecoder().decode(base64Data);

        try {
            Files.copy(new ByteArrayInputStream(data), tmpFilePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return tmpFilePath.toAbsolutePath().toString();
    }


}
