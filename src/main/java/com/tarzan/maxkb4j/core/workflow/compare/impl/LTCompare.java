package com.tarzan.maxkb4j.core.workflow.compare.impl;

import com.tarzan.maxkb4j.core.workflow.compare.Compare;

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
        if (sourceValue instanceof Float){
            return (float) sourceValue<  Float.parseFloat(targetValue);
        }
        if (sourceValue instanceof Double){
            return (double) sourceValue< Double.parseDouble(targetValue);
        }
        if (sourceValue instanceof Integer){
            return (int) sourceValue< Integer.parseInt(targetValue);
        }
        return false;
    }
}
