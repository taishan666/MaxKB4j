package com.tarzan.maxkb4j.module.application.workflow.node.imageunderstand.dto;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.application.workflow.dto.BaseParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ImageUnderstandParams extends BaseParams {
    @JsonProperty("model_id")
    private String modelId;
    private String system;
    private String prompt;
    @JsonProperty("dialogue_number")
    private Integer dialogueNumber;
    @JsonProperty("dialogue_type")
    private String dialogueType;
    @JsonProperty("is_result")
    private Boolean isResult;
    private List<JSONObject> imageList;
    @JsonProperty("model_params_setting")
    private JSONObject modelParamsSetting;

    @Override
    public boolean isValid() {
        return false;
    }
}
