package com.maxkb4j.workflow.compare.impl;


import com.maxkb4j.workflow.compare.Compare;

import java.util.List;

public class NotContainCompare implements Compare {
    @Override
    public boolean support(String compare) {
        return compare.equals("not_contain");
    }

    @Override
    public boolean compare(Object sourceValue,  String targetValue) {
        if (sourceValue instanceof List<?>) {
            return !((List<?>) sourceValue).contains(targetValue);
        }
        if(sourceValue instanceof String){
            return !((String) sourceValue).contains(targetValue);
        }
        return false;
    }
}
