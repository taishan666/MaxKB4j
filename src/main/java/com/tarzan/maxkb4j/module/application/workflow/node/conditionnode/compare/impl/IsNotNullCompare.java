package com.tarzan.maxkb4j.module.application.workflow.node.conditionnode.compare.impl;


import com.tarzan.maxkb4j.module.application.workflow.node.conditionnode.compare.Compare;

import java.util.Objects;

public class IsNotNullCompare implements Compare {
    @Override
    public boolean support(String compare) {
        return compare.equals("is_not_null");
    }

    @Override
    public boolean compare(Object sourceValue, String targetValue) {
        return Objects.nonNull(sourceValue)&&!sourceValue.equals("");
    }
}
