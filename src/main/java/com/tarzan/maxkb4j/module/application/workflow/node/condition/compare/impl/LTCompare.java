package com.tarzan.maxkb4j.module.application.workflow.node.condition.compare.impl;

import com.tarzan.maxkb4j.module.application.workflow.node.condition.compare.Compare;

public class LTCompare implements Compare {
    @Override
    public boolean support(String compare) {
        return compare.equals("lt");
    }

    @Override
    public boolean compare(Object sourceValue,  String targetValue) {
        return (float) sourceValue <= Float.parseFloat(targetValue);
    }
}
