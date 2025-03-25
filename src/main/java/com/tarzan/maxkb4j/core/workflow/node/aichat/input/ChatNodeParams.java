package com.tarzan.maxkb4j.core.workflow.node.aichat.input;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.dto.BaseParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChatNodeParams extends BaseParams {

    private String modelId;
    private String system;
    private String prompt;
    private int dialogueNumber;
    private Boolean isResult;
    private JSONObject modelParamsSetting;
    private String dialogueType;

    @Override
    public boolean isValid() {
        return false;
    }
}
