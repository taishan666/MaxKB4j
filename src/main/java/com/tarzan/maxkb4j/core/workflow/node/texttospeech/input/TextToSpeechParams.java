package com.tarzan.maxkb4j.core.workflow.node.texttospeech.input;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.List;

@Data
public class TextToSpeechParams {

    private String ttsModelId;
    private List<String> contentList;
    private JSONObject modelParamsSetting;
}
