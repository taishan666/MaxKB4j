package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.model;

import com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.dashscope.utils.JsonUtils;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseTextToSpeech;
import com.tarzan.maxkb4j.util.StringUtil;
import io.reactivex.Flowable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Base64;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class QWenTTS extends BaseTextToSpeech implements BaseModel<BaseTextToSpeech> {
    private MultiModalConversationParam param;

    public QWenTTS(MultiModalConversationParam param) {
        super();
        this.param = param;
    }

    @Override
    public BaseTextToSpeech build(String modelName, ModelCredential modelCredential, JSONObject params) {
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model(modelName)
                .apiKey(modelCredential.getApiKey())
                .voice(AudioParameters.Voice.CHERRY)
                .build();
        return new QWenTTS(param);
    }

    @Override
    public byte[] textToSpeech(String text) {
        MultiModalConversation conv = new MultiModalConversation();
        param.setText(text);
        MultiModalConversationResult result;
        try {
            result = conv.call(param);
            String audioUrl = result.getOutput().getAudio().getUrl();
            System.out.print(audioUrl);
            return  downloadAudio(audioUrl);
        } catch (NoApiKeyException | UploadFileException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] downloadAudio(String audioUrl) throws IOException {
        URL url = new URL(audioUrl);
        try (InputStream inputStream = url.openStream();
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            return buffer.toByteArray();
        }
    }

    public byte[] textToSpeech1(String text) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MultiModalConversation conv = new MultiModalConversation();
        param.setText(text);
        Flowable<MultiModalConversationResult> result;
        try {
            result = conv.streamCall(param);
        } catch (NoApiKeyException | UploadFileException e) {
            throw new RuntimeException(e);
        }
        result.blockingForEach(r -> {
            System.out.println("result: " + JsonUtils.toJson(r));
            String dataStr= r.getOutput().getAudio().getData();
            if (StringUtil.isNotBlank(dataStr)){
                //Base64 解码为字节流
                byte[] byteArray = Base64.getDecoder().decode(dataStr);
                System.out.println("byteArray  length:  " + byteArray.length);
                outputStream.write(byteArray);
            }
        });
        System.out.println("outputStream  length:  " + outputStream.toByteArray().length);
        return outputStream.toByteArray();
    }
}