package com.tarzan.maxkb4j.core.workflow.node.imagegenerate.input;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.dto.BaseParams;
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
