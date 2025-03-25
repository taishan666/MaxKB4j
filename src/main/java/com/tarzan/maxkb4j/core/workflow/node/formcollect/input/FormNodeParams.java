package com.tarzan.maxkb4j.core.workflow.node.formcollect.input;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.dto.BaseParams;
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
