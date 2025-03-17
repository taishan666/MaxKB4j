package com.tarzan.maxkb4j.module.application.workflow.node.imageunderstand.input;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.application.workflow.dto.BaseParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ImageUnderstandParams extends BaseParams {
    private String modelId;
    private String system;
    private String prompt;
    private Integer dialogueNumber;
    private String dialogueType;
    private Boolean isResult;
    private List<String> imageList;
    private JSONObject modelParamsSetting;

    @Override
    public boolean isValid() {
        return false;
    }
}
