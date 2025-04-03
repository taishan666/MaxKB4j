package com.tarzan.maxkb4j.core.workflow.node.formcollect.input;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.List;

@Data
public class FormNodeParams {
    private List<JSONObject> formFieldList;
    private String formContentFormat;
    private JSONObject formData;
}
