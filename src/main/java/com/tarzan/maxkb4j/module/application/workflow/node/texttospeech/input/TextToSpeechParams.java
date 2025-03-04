package com.tarzan.maxkb4j.module.application.workflow.node.texttospeech.input;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.application.workflow.dto.BaseParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class TextToSpeechParams extends BaseParams {

    @JsonProperty("tts_model_id")
    private String modelId;
    @JsonProperty("is_result")
    private Boolean isResult;
    @JsonProperty("content_list")
    private List<String> contentList;
    @JsonProperty("model_params_setting")
    private JSONObject modelParamsSetting;
    @Override
    public boolean isValid() {
        return false;
    }
}
