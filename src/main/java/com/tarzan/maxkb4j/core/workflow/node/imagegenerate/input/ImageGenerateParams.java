package com.tarzan.maxkb4j.core.workflow.node.imagegenerate.input;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
public class ImageGenerateParams {

    private String modelId;
    private String prompt;
    private String negativePrompt;
    private Integer dialogueNumber;
    private String dialogueType;
    private Boolean isResult;
    private JSONObject modelParamsSetting;

}
