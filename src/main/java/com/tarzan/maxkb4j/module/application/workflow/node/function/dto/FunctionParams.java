package com.tarzan.maxkb4j.module.application.workflow.node.function.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.application.workflow.dto.BaseParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class FunctionParams extends BaseParams {
    @JsonProperty("input_field_list")
    private List<Map<String,Object>> inputFieldList;
    private String code;
    @JsonProperty("is_result")
    private Boolean isResult;
}
