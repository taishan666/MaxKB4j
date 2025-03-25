package com.tarzan.maxkb4j.core.workflow.node.condition.compare.impl;

import com.tarzan.maxkb4j.core.workflow.node.condition.compare.Compare;

import java.util.Objects;

public class IsNullCompare implements Compare {
    @Override
    public boolean support(String compare) {
        return compare.equals("is_null");
    }

    @Override
    public boolean compare(Object sourceValue, String targetValue) {
        return Objects.isNull(sourceValue)||sourceValue.equals("");
    }
}
