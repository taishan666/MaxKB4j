package com.tarzan.maxkb4j.module.model.provider.service.impl.aliyunModelProvider.model;

import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.OSSUtils;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.internal.Utils;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class WanXImageModel implements ImageModel {
    private final ImageSynthesisParam.ImageSynthesisParamBuilder<?, ?> paramBuilder;

    public WanXImageModel(ImageSynthesisParam.ImageSynthesisParamBuilder<?, ?> paramBuilder) {
        this.paramBuilder = paramBuilder;
    }

    @Override
    public Response<Image> generate(String prompt) {
        ImageSynthesisParam param = paramBuilder.prompt(prompt).build();
        ImageSynthesis imageSynthesis = new ImageSynthesis();
        try {
            ImageSynthesisResult result = imageSynthesis.call(param);
            return Response.from(imagesFrom(result).get(0));
        } catch (NoApiKeyException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public Response<List<Image>> generate(String prompt, int n) {
        ImageSynthesisParam param = paramBuilder.prompt(prompt).n(n).build();
        ImageSynthesis imageSynthesis = new ImageSynthesis();
        try {
            ImageSynthesisResult result = imageSynthesis.call(param);
            return Response.from(imagesFrom(result));
        } catch (NoApiKeyException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public Response<Image> edit(Image image, String prompt) {
        ImageSynthesisParam param = paramBuilder.prompt(prompt).build();
        String imageUrl = imageUrl(image, param.getModel(), param.getApiKey());
        if (imageUrl.startsWith("oss://")) {
            Map<String, Object> headers=new HashMap<>();
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
        ImageSynthesisParam param = paramBuilder.prompt(prompt).build();
        String imageUrl = imageUrl(image, param.getModel(), param.getApiKey());
        String maskUrl = imageUrl(mask, param.getModel(), param.getApiKey());
        Map<String, Object> headers=new HashMap<>();
        if (imageUrl.startsWith("oss://")||maskUrl.startsWith("oss://")) {
            headers.put("X-DashScope-OssResourceResolve", "enable");
        }
        param.setBaseImageUrl(imageUrl);
       // param.setMaskUrl(maskUrl);
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
