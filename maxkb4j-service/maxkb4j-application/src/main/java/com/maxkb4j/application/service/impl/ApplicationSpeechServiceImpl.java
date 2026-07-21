package com.maxkb4j.application.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.application.service.IApplicationService;
import com.maxkb4j.application.service.IApplicationSpeechService;
import com.maxkb4j.application.vo.ApplicationVO;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.model.service.ISTTModel;
import com.maxkb4j.model.service.ITTSModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;

/**
 * 应用语音（TTS/STT）相关逻辑，从 {@link ApplicationServiceImpl} 抽离。
 *
 * @author tarzan
 */
@RequiredArgsConstructor
@Service
public class ApplicationSpeechServiceImpl implements IApplicationSpeechService {

    private final IModelProviderService modelFactory;
    private final IApplicationService applicationService;

    /**
     * 语音播放测试：使用传入的模型参数直接合成固定测试文案。
     */
    public byte[] playDemoText(JSONObject modelParams) {
        String ttsModelId = modelParams.getString("ttsModelId");
        ITTSModel ttsModel = modelFactory.buildTTSModel(ttsModelId, modelParams);
        return ttsModel.textToSpeech("你好，这里是语音播放测试");
    }

    @Override
    public String speechToText(String appId, MultipartFile file, boolean debug) throws IOException {
        ApplicationVO app =applicationService.getAppDetail(appId, debug);
        return speechToText(app.getSttModelId(), file);
    }

    @Override
    public byte[] textToSpeech(String appId, JSONObject data, boolean debug) {
        String text = data.getString("text");
        ApplicationVO app = applicationService.getAppDetail(appId, debug);
        return this.textToSpeech(text,app);
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
        ITTSModel ttsModel = modelFactory.buildTTSModel(app.getTtsModelId(), app.getTtsModelParamsSetting());
        return ttsModel.textToSpeech(text);
    }

    /**
     * 语音转文本。
     */
    private String speechToText(String sttModelId, MultipartFile file) throws IOException {
        ISTTModel sttModel = modelFactory.buildSTTModel(sttModelId, new JSONObject());
        String suffix = Objects.requireNonNull(file.getContentType()).split("/")[1];
        return sttModel.speechToText(file.getBytes(), suffix);
    }
}
