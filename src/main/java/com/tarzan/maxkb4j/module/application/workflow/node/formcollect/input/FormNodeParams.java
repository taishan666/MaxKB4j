package com.tarzan.maxkb4j.module.application.workflow.node.formcollect.input;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.application.workflow.dto.BaseParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class FormNodeParams extends BaseParams {
    private List<JSONObject> formFieldList;
    private String formContentFormat;
    private JSONObject formData;
}
