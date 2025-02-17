package com.tarzan.maxkb4j.module.application.workflow.node.questionnode.dto;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.application.workflow.dto.BaseParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class QuestionParams extends BaseParams {
    @JsonProperty("model_id")
    private String modelId;
    private String system;
    private String prompt;
    @JsonProperty("dialogue_number")
    private Integer dialogueNumber;
    @JsonProperty("is_result")
    private Boolean isResult;
    @JsonProperty("model_params_setting")
    private JSONObject modelParamsSetting;
    @Override
    public boolean isValid() {
        return false;
    }
}
