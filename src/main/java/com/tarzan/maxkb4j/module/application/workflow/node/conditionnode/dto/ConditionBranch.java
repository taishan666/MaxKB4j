package com.tarzan.maxkb4j.module.application.workflow.node.conditionnode.dto;

import lombok.Data;

import java.util.List;

@Data
public class ConditionBranch {
    private String id;
    private String type;
    private String condition;
    private List<Condition> conditions;
}
