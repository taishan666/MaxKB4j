package com.maxkb4j.workflow.compare.impl;


import com.maxkb4j.workflow.compare.Compare;

public class IsNotTrueCompare implements Compare {
    @Override
    public boolean support(String compare) {
        return compare.equals("is_not_true");
    }

    @Override
    public boolean compare(Object sourceValue, String targetValue) {
        return !((boolean) sourceValue);
    }
}
