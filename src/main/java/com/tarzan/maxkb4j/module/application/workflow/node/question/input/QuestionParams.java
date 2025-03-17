package com.tarzan.maxkb4j.module.application.workflow.node.question.input;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.application.workflow.dto.BaseParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class QuestionParams extends BaseParams {
    private String modelId;
    private String system;
    private String prompt;
    private Integer dialogueNumber;
    private Boolean isResult;
    private JSONObject modelParamsSetting;
    @Override
    public boolean isValid() {
        return false;
    }
}
