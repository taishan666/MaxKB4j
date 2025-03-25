package com.tarzan.maxkb4j.core.workflow.node.speechtotext.input;

import com.tarzan.maxkb4j.core.workflow.dto.BaseParams;
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
