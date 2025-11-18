package com.tarzan.maxkb4j.core.workflow.compare.impl;

import com.tarzan.maxkb4j.core.workflow.compare.Compare;

import java.util.List;

public class ContainCompare implements Compare {
    @Override
    public boolean support(String compare) {
        return compare.equals("contain");
    }

    @Override
    public boolean compare(Object sourceValue, String targetValue) {
        if(sourceValue instanceof List<?>){
            return ((List<?>) sourceValue).contains(targetValue);
        }
        if(sourceValue instanceof String){
            return ((String) sourceValue).contains(targetValue);
        }
        return false;
    }
}
