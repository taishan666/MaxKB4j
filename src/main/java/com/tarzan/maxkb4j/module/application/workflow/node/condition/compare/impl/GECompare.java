package com.tarzan.maxkb4j.module.application.workflow.node.condition.compare.impl;

import com.tarzan.maxkb4j.module.application.workflow.node.condition.compare.Compare;

public class GECompare implements Compare {
    @Override
    public boolean support(String compare) {
        return compare.equals("ge");
    }
    @Override
    public boolean compare(Object sourceValue, String targetValue) {
        return (float) sourceValue>= Float.parseFloat(targetValue);
    }
}
