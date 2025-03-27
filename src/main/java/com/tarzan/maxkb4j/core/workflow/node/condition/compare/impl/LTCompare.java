package com.tarzan.maxkb4j.core.workflow.node.condition.compare.impl;

import com.tarzan.maxkb4j.core.workflow.node.condition.compare.Compare;

import java.util.Collection;

public class LTCompare implements Compare {
    @Override
    public boolean support(String compare) {
        return compare.equals("lt");
    }

    @Override
    public boolean compare(Object sourceValue,  String targetValue) {
        if (sourceValue instanceof Collection){
            return ((Collection<?>) sourceValue).size()< Integer.parseInt(targetValue);
        }
        return (float) sourceValue < Float.parseFloat(targetValue);
    }
}
