package com.tarzan.maxkb4j.core.workflow.node.toollib.input;

import com.tarzan.maxkb4j.module.tool.domain.dto.ToolInputField;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ToolLibNodeParams {
    private List<ToolInputField> inputFieldList;
    private String code;
    private Map<String,Object> initParams;
    private Boolean isResult;
}
