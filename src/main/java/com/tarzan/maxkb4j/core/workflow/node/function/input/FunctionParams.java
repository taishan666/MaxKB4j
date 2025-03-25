package com.tarzan.maxkb4j.core.workflow.node.function.input;

import com.tarzan.maxkb4j.core.workflow.dto.BaseParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class FunctionParams extends BaseParams {
    private List<Map<String,Object>> inputFieldList;
    private String code;
    private Boolean isResult;
}
