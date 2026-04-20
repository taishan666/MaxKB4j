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
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
                .parameters(parameters)
                .sampleRate(16000)
                .format("mp3")
                .build();
    }

    @Override
    public String speechToText(byte[] audioBytes, String suffix) {
        log.info("========== 语音识别开始 ==========");
        log.info("使用模型: {}", modelName);
        log.info("音频数据大小: {} bytes", audioBytes.length);
        log.info("文件后缀: {}", suffix);
        int sampleRate;
        Path tempFile=null;
        try {
            tempFile = Files.createTempFile("audio_temp_", "."+suffix);
            // 2. 将 byte[] 写入临时文件
            // 使用 WRITE 和 TRUNCATE_EXISTING 确保覆盖写入
            Files.write(tempFile, audioBytes, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            // 3. 读取音频文件
            // 注意：AudioFileIO.read 通常接受 File 对象，所以需要将 Path 转为 File
            File fileObj = tempFile.toFile();
            AudioFile audioFile = AudioFileIO.read(fileObj);
            sampleRate = audioFile.getAudioHeader().getSampleRateAsNumber();

        } catch (ReadOnlyFileException | IOException | CannotReadException | TagException | InvalidAudioFrameException e) {
            throw new RuntimeException(e);
        } finally {
            // 4. 删除临时文件
            // 无论成功还是失败，都在 finally 块中尝试删除文件，防止磁盘垃圾堆积
            try {
                if (tempFile!=null&&Files.exists(tempFile)) {
                    Files.delete(tempFile);
                }
            } catch (IOException e) {
                log.error("删除临时文件失败: {}", e.getMessage());
            }
        }
        this.param.setSampleRate(sampleRate);
        String format = suffix != null ? suffix.toLowerCase() : "mp3";
        this.param.setFormat(format);
        log.info("使用格式: {}, 采样率: {}", format, sampleRate);
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
