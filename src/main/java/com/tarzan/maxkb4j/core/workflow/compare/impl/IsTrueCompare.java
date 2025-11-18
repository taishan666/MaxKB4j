package com.tarzan.maxkb4j.core.workflow.compare.impl;

import com.tarzan.maxkb4j.core.workflow.compare.Compare;

public class IsTrueCompare implements Compare {
    @Override
    public boolean support(String compare) {
        return compare.equals("is_true");
    }

    @Override
    public boolean compare(Object sourceValue, String targetValue) {
        return (boolean)sourceValue;
    }
}
