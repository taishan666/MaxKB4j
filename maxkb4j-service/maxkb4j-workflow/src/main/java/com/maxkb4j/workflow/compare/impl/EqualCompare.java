package com.maxkb4j.workflow.compare.impl;


import com.maxkb4j.workflow.compare.Compare;

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
