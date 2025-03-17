package com.tarzan.maxkb4j.module.application.workflow.node.speechtotext.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.application.workflow.dto.BaseParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class SpeechToTextParams extends BaseParams {

    private String sttModelId;
    private Boolean isResult;
    private List<String> audioList;

    @Override
    public boolean isValid() {
        return false;
    }
}
