package com.tarzan.maxkb4j.module.model.provider.service.impl.aliyunModelProvider.model;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.service.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.service.impl.BaseSpeechToText;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class ParaFormerRealtimeSTT extends BaseSpeechToText implements BaseModel<BaseSpeechToText> {

    private RecognitionParam param;

    @Override
    public BaseSpeechToText build(String modelName, ModelCredential credential, JSONObject params) {
        Map<String, Object> parameters=new HashMap<>();
        parameters.put("language_hints", new String[]{"zh", "en"});
        this.param =
                RecognitionParam.builder()
                        .apiKey(credential.getApiKey())
                        .model(modelName)
                        .format("wav")
                        .sampleRate(16000)
                        .parameters(parameters)
                        .build();
        return  this;
    }

    @Override
    public String speechToText(byte[] audioBytes, String suffix) {
        // 创建Recognition实例
        Recognition recognizer = new Recognition();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> text = new AtomicReference<>("");
        ResultCallback<RecognitionResult> callback = new ResultCallback<>() {
            @Override
            public void onEvent(RecognitionResult result) {
                if (result.isSentenceEnd()) {
                    text.set(result.getSentence().getText());
                }
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                System.out.println("RecognitionCallback error: " + e.getMessage());
            }
        };
        // 将录音音频数据发送给流式识别服务
        recognizer.call(param, callback);
        try {
            //  ByteBuffer buffer = ByteBuffer.wrap(audioBytes);
            InputStream fis = new ByteArrayInputStream(audioBytes);
            // chunk size set to 1 seconds for 16KHz sample rate
            byte[] buffer = new byte[3200];
            int bytesRead;
            // Loop to read chunks of the file
            while ((bytesRead = fis.read(buffer)) != -1) {
                ByteBuffer byteBuffer;
                // Handle the last chunk which might be smaller than the buffer size
                if (bytesRead < buffer.length) {
                    byteBuffer = ByteBuffer.wrap(buffer, 0, bytesRead);
                } else {
                    byteBuffer = ByteBuffer.wrap(buffer);
                }
                recognizer.sendAudioFrame(byteBuffer);
                buffer = new byte[3200];
            }
            recognizer.stop();
            latch.await();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
        return text.get();
    }

}
