package com.maxkb4j.workflow.compare.impl;


import com.maxkb4j.workflow.compare.Compare;

public class IsTrueCompare implements Compare {
    @Override
    public boolean support(String compare) {
        return compare.equals("is_true");
    }

    @Override
    public boolean compare(Object sourceValue, String targetValue) {
        if (sourceValue instanceof Boolean) {
            return  (Boolean) sourceValue;
        } else if (sourceValue instanceof String) {
            return Boolean.parseBoolean((String) sourceValue); // 或 Boolean.parseBoolean((String) value)
        } else {
            return false;
        }
    }
}
