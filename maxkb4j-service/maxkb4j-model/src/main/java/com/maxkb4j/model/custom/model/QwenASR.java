package com.maxkb4j.model.custom.model;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.mp.entity.ModelCredential;
import com.maxkb4j.model.service.STTModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QwenASR implements STTModel {

    private String modelName;
    private ModelCredential credential;
    private JSONObject params;

    public QwenASR(String modelName, ModelCredential modelCredential, JSONObject params) {
       this.modelName=modelName;
       this.credential=modelCredential;
       this.params=params;
    }
    @Override
    public String speechToText(byte[] audioBytes, String suffix) {
        MultiModalConversation conv = new MultiModalConversation();
        MultiModalMessage userMessage = MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(List.of(Collections.singletonMap("audio", "https://dashscope.oss-cn-beijing.aliyuncs.com/audios/welcome.mp3")))
                .build();

        MultiModalMessage sysMessage = MultiModalMessage.builder().role(Role.SYSTEM.getValue()).build();
        Map<String, Object> asrOptions = new HashMap<>();
        asrOptions.put("enable_itn", false);
        // asrOptions.put("language", "zh"); // 可选，若已知音频的语种，可通过该参数指定待识别语种，以提升识别准确率
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .apiKey(System.getenv(credential.getApiKey()))
                .model(modelName)
                .message(sysMessage)
                .message(userMessage)
                .parameter("asr_options", asrOptions)
                .build();
        try {
            MultiModalConversationResult result = conv.call(param);
           return (String) result.getOutput().getChoices().get(0).getMessage().getContent().get(0).get("text");
        } catch (NoApiKeyException | UploadFileException e) {
            throw new RuntimeException(e);
        }
    }
}
