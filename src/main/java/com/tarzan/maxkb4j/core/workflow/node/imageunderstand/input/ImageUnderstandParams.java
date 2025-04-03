package com.tarzan.maxkb4j.core.workflow.node.imageunderstand.input;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
public class ImageUnderstandParams  {
    private String modelId;
    private String system;
    private String prompt;
    private Integer dialogueNumber;
    private String dialogueType;
    private Boolean isResult;
    private List<String> imageList;
    private JSONObject modelParamsSetting;

}
