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

    private String modelId;
    private Boolean isResult;
    private List<String> contentList;
    private JSONObject modelParamsSetting;
    @Override
    public boolean isValid() {
        return false;
    }
}
