package com.tarzan.maxkb4j.module.application.workflow.node.condition.compare.impl;

import com.tarzan.maxkb4j.module.application.workflow.node.condition.compare.Compare;

public class EqualCompare implements Compare {
    @Override
    public boolean support(String compare) {
        return compare.equals("eq");
    }

    @Override
    public boolean compare(Object sourceValue, String targetValue) {
        return sourceValue.equals(targetValue);
    }
}
