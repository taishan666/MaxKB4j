package com.tarzan.maxkb4j.core.workflow.node.question.input;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
public class QuestionParams {
    private String modelId;
    private String system;
    private String prompt;
    private Integer dialogueNumber;
    private JSONObject modelParamsSetting;
}
