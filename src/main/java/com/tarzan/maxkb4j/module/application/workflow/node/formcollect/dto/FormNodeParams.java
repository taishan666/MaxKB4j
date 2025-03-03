package com.tarzan.maxkb4j.module.application.workflow.node.formcollect.dto;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.application.workflow.dto.BaseParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class FormNodeParams extends BaseParams {
    @JsonProperty("form_field_list")
    private List<JSONObject> formFieldList;
    @JsonProperty("form_content_format")
    private String formContentFormat;
    @JsonProperty("form_data")
    private JSONObject formData;
}
