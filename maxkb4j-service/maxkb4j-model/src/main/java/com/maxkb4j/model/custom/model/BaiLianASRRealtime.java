package com.maxkb4j.model.custom.model;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.mp.entity.ModelCredential;
import com.maxkb4j.model.service.STTModel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Data
public class BaiLianASRRealtime implements STTModel {


    private RecognitionParam param;
    private String modelName;
    private ModelCredential credential;

    private static final List<String> SUPPORT_MODELS = List.of("fun-asr-realtime", "paraformer-realtime-v2", "gummy-realtime-v1");

    public BaiLianASRRealtime(String modelName, ModelCredential credential, JSONObject params) {
        this.modelName = modelName;
        this.credential = credential;
        this.param = buildParam();
    }

    private RecognitionParam buildParam() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("language_hints", new String[]{"zh", "en"});
        if ("fun-asr-realtime".equals(modelName)) {
            parameters.put("disfluency_removal_enabled", false);
            parameters.put("show_punctuation", true);
            parameters.put("inverse_text_normalization", true);
        } else if ("paraformer-realtime-v2".equals(modelName)) {
            parameters.put("disfluency_removal_enabled", false);
        }
        return RecognitionParam.builder()
                .apiKey(credential.getApiKey())
                .model(modelName)
                .format("mp3")
                .sampleRate(22050)
                .parameters(parameters)
                .build();
    }

    @Override
    public String speechToText(byte[] audioBytes, String suffix) {
        log.info("========== 语音识别开始 ==========");
        log.info("使用模型: {}", modelName);
        log.info("音频数据大小: {} bytes", audioBytes.length);
        log.info("文件后缀: {}", suffix);

        String format = suffix != null ? suffix.toLowerCase() : "wav";
        this.param.setFormat(format);
        log.info("使用格式: {}, 采样率: {}", format, param.getSampleRate());
        AtomicReference<String> resultText = new AtomicReference<>("");
        ResultCallback<RecognitionResult> callback = new ResultCallback<>() {
            @Override
            public void onEvent(RecognitionResult message) {
                if (message.isSentenceEnd()) {
                    resultText.set(message.getSentence().getText());
                }
            }

            @Override
            public void onComplete() {
            }

            @Override
            public void onError(Exception e) {
                log.error(e.getMessage());
            }
        };

        Recognition recognizer = new Recognition();
        recognizer.call(param, callback);
        int sendFrameLength = 3200;
        for (int i = 0; i * sendFrameLength < audioBytes.length; i ++) {
            int start = i * sendFrameLength;
            int end = Math.min(start + sendFrameLength, audioBytes.length);
            ByteBuffer byteBuffer = ByteBuffer.wrap(audioBytes, start, end - start);
            recognizer.sendAudioFrame(byteBuffer);
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        recognizer.stop();
        log.info("最终识别文本: [{}]", resultText.get());
        log.info("========== 语音识别结束 ==========");
        return resultText.get();
    }

}
