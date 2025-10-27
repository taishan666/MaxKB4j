package com.tarzan.maxkb4j.core.workflow.compare.impl;

import com.tarzan.maxkb4j.core.workflow.compare.Compare;

import java.util.List;

public class LengthGTCompare implements Compare {
    @Override
    public boolean support(String compare) {
        return compare.equals("len_gt");
    }

    @Override
    public boolean compare(Object sourceValue, String targetValue) {
        int target = Integer.parseInt(targetValue);
        if (sourceValue instanceof List<?>) {
            return ((List<?>) sourceValue).size() > target;
        }
        if (sourceValue instanceof String) {
            return ((String) sourceValue).length() > target;
        }
        return false;
    }
}
