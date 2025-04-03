package com.tarzan.maxkb4j.core.workflow.node.aichat.input;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.List;

@Data
public class ChatNodeParams{

    private String modelId;
    private String system;
    private String prompt;
    private List<String> questionReferenceAddress;
    private List<String> datasetReferenceAddress;
    private int dialogueNumber;
    private Boolean isResult;
    private JSONObject modelParamsSetting;
    private String dialogueType;

}
