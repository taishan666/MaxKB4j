package com.tarzan.maxkb4j.module.application.workflow.node.conditionnode.compare.impl;

import com.tarzan.maxkb4j.module.application.workflow.node.conditionnode.compare.Compare;

import java.util.List;

public class NotContainCompare implements Compare {
    @Override
    public boolean support(String compare) {
        return compare.equals("not_contain");
    }

    @Override
    public boolean compare(Object sourceValue,  String targetValue) {
        if (sourceValue instanceof List<?>) {
            return !((List<?>) sourceValue).contains(targetValue);
        }
        return false;
    }
}
