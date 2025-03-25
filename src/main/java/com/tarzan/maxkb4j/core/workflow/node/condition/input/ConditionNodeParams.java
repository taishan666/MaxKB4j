package com.tarzan.maxkb4j.core.workflow.node.condition.input;

import com.tarzan.maxkb4j.core.workflow.dto.BaseParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ConditionNodeParams extends BaseParams {
    private List<ConditionBranch> branch;

    @Override
    public boolean isValid() {
        return false;
    }
}

