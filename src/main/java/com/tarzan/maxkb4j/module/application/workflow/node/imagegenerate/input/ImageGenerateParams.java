package com.tarzan.maxkb4j.module.application.workflow.node.imagegenerate.input;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.application.workflow.dto.BaseParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ImageGenerateParams extends BaseParams {

    private String modelId;
    private String prompt;
    private String negativePrompt;
    private Integer dialogueNumber;
    private String dialogueType;
    private Boolean isResult;
    private JSONObject modelParamsSetting;

    @Override
    public boolean isValid() {
        return false;
    }
}
