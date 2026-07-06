package com.maxkb4j.workflow.model;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class ModelConfig {
    private String modelId;
    private JSONObject modelParamsSetting;
}
