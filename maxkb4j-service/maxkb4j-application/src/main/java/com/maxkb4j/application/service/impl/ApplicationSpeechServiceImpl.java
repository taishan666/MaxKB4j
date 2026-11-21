package com.maxkb4j.application.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.application.service.IApplicationInternalService;
import com.maxkb4j.application.service.IApplicationSpeechService;
import com.maxkb4j.application.vo.ApplicationVO;
import com.maxkb4j.model.base.STTModel;
import com.maxkb4j.model.base.TTSModel;
import com.maxkb4j.model.service.IModelProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 应用语音（TTS/STT）相关逻辑，从 {@link ApplicationServiceImpl} 抽离。
 *
 * @author tarzan
 */
@RequiredArgsConstructor
@Service
public class ApplicationSpeechServiceImpl implements IApplicationSpeechService {

    /**
     * 合法音频扩展名，避免把垃圾后缀直接写进临时文件名。
     */
    private static final Pattern AUDIO_SUFFIX_PATTERN = Pattern.compile("[a-zA-Z0-9]{1,16}");

    private final IModelProviderService modelFactory;
    private final IApplicationInternalService applicationService;

    /**
     * 语音播放测试：使用传入的模型参数直接合成固定测试文案。
     */
    public byte[] playDemoText(JSONObject modelParams) {
        String ttsModelId = modelParams.getString("ttsModelId");
        TTSModel ttsModel = modelFactory.buildTTSModel(ttsModelId, modelParams);
        return ttsModel.textToSpeech("你好，这里是语音播放测试");
    }

    @Override
    public String speechToText(String appId, MultipartFile file, boolean debug) throws IOException {
        ApplicationVO app = applicationService.getAppDetail(appId, debug);
        return speechToText(app.getSttModelId(), file);
    }

    @Override
    public byte[] textToSpeech(String appId, JSONObject data, boolean debug) {
        String text = data.getString("text");
        ApplicationVO app = applicationService.getAppDetail(appId, debug);
        return this.textToSpeech(text, app);
    }

    /**
     * 文本转语音。浏览器内置类型或未配置模型时返回空字节数组。
     */
    private byte[] textToSpeech(String text, ApplicationVO app) {
        if ("BROWSER".equals(app.getTtsType())) {
            return new byte[0];
        }
        if (app.getTtsModelId() == null) {
            return new byte[0];
        }
        TTSModel ttsModel = modelFactory.buildTTSModel(app.getTtsModelId(), app.getTtsModelParamsSetting());
        return ttsModel.textToSpeech(text);
    }

    /**
     * 语音转文本。
     */
    private String speechToText(String sttModelId, MultipartFile file) throws IOException {
        STTModel sttModel = modelFactory.buildSTTModel(sttModelId, new JSONObject());
        return sttModel.speechToText(file.getBytes(), resolveAudioSuffix(file));
    }

    /**
     * 解析音频格式：优先取原始文件名扩展名，其次取 Content-Type 子类型。
     * 不能直接拿 MIME 子类型当音频格式（audio/mpeg 的子类型是 mpeg 而非 mp3），
     * Content-Type 也可能缺失或不含 "/"，这些情况返回 null，
     * 由模型侧 {@code normalizeFormat} 归一化为支持的格式或默认值。
     */
    private String resolveAudioSuffix(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename != null) {
            int dot = filename.lastIndexOf('.');
            if (dot >= 0 && dot < filename.length() - 1) {
                String ext = filename.substring(dot + 1).trim();
                if (AUDIO_SUFFIX_PATTERN.matcher(ext).matches()) {
                    return ext.toLowerCase(Locale.ROOT);
                }
            }
        }
        String contentType = file.getContentType();
        if (contentType != null) {
            int slash = contentType.indexOf('/');
            if (slash >= 0 && slash < contentType.length() - 1) {
                // 去掉参数部分，如 "audio/webm;codecs=opus" → "webm"
                String subtype = contentType.substring(slash + 1).split(";")[0].trim();
                if (!subtype.isEmpty()) {
                    return subtype.toLowerCase(Locale.ROOT);
                }
            }
        }
        return null;
    }
}
