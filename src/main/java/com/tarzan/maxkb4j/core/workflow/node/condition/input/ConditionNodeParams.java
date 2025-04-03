package com.tarzan.maxkb4j.core.workflow.node.condition.input;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
public class ConditionNodeParams {
    private List<ConditionBranch> branch;
}

