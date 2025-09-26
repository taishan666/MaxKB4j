package com.tarzan.maxkb4j.core.workflow.node.tool.input;

import com.tarzan.maxkb4j.module.tool.domain.dto.ToolInputField;
import lombok.Data;

import java.util.List;

@Data
public class ToolNodeParams {
    private List<ToolInputField> inputFieldList;
    private String code;
    private Boolean isResult;
}
