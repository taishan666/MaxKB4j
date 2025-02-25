package com.tarzan.maxkb4j.module.application.workflow.node.speechToTextNode.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.application.workflow.dto.BaseParams;
import lombok.Data;

import java.util.List;

@Data
public class SpeechToTextParams extends BaseParams {

    @JsonProperty("stt_model_id")
    private String sttModelId;
    @JsonProperty("is_result")
    private Boolean isResult;
    @JsonProperty("audio_list")
    private List<String> audioList;

    @Override
    public boolean isValid() {
        return false;
    }
}
