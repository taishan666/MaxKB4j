package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.model;

import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.tarzan.maxkb4j.module.model.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseSpeechToText;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

@EqualsAndHashCode(callSuper = true)
@Data
public class BaiLianSpeechToText extends BaseSpeechToText implements BaseModel {

    private String apiBase;
    private String apiKey;


    public BaiLianSpeechToText(String apiBase, String apiKey) {
        super();
        this.apiBase = apiBase;
        this.apiKey = apiKey;
    }

    @Override
    public <T> T newInstance(String modelName, ModelCredential credential) {
        return (T) new BaiLianSpeechToText(credential.getApiBase(),credential.getApiKey());
    }

    @Override
    public String speechToText(File audioFile) {
        SpeechSynthesisParam param =
                SpeechSynthesisParam.builder()
                        // 若没有将API Key配置到环境变量中，需将下面这行代码注释放开，并将apiKey替换为自己的API Key
                        // .apiKey(apikey)
                        .model("")
                        .voice("")
                        .build();
        SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, null);
        ByteBuffer audio = synthesizer.call("今天天气怎么样？");
        File file = new File("output.mp3");
        System.out.print("requestId: " + synthesizer.getLastRequestId());
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(audio.array());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return "";
    }
}
