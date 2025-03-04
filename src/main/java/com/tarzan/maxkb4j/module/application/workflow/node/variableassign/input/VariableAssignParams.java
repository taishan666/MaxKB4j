package com.tarzan.maxkb4j.module.application.workflow.node.variableassign.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.application.workflow.dto.BaseParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class VariableAssignParams extends BaseParams {

    @JsonProperty("variable_list")
    private List<Map<String, Object>> variableList;

    @Override
    public boolean isValid() {
        return false;
    }
}
