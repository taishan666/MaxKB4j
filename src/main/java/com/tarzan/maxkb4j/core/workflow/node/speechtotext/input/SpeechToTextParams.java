package com.tarzan.maxkb4j.core.workflow.node.speechtotext.input;

import lombok.Data;

import java.util.List;

@Data
public class SpeechToTextParams {

    private String sttModelId;
    private Boolean isResult;
    private List<String> audioList;

}
