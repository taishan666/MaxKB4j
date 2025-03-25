package com.tarzan.maxkb4j.core.workflow.node.variableassign.input;

import com.tarzan.maxkb4j.core.workflow.dto.BaseParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class VariableAssignParams extends BaseParams {

    private List<Map<String, Object>> variableList;

    @Override
    public boolean isValid() {
        return false;
    }
}
