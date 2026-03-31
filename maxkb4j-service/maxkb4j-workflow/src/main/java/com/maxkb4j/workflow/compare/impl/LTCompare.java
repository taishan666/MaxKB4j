package com.maxkb4j.workflow.compare.impl;


import com.maxkb4j.workflow.annotation.CompareType;
import com.maxkb4j.workflow.compare.Compare;
import com.maxkb4j.workflow.enums.CompareOperator;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@CompareType(CompareOperator.LT)
public class LTCompare implements Compare {

    @Override
    public boolean compare(Object sourceValue, Object targetValue) {
        if (sourceValue == null || targetValue == null) {
            return false;
        }
        String targetStr = targetValue.toString();
        if (sourceValue instanceof Collection) {
            return ((Collection<?>) sourceValue).size() < Integer.parseInt(targetStr);
        }
        if (sourceValue instanceof Float) {
            return (float) sourceValue < Float.parseFloat(targetStr);
        }
        if (sourceValue instanceof Double) {
            return (double) sourceValue < Double.parseDouble(targetStr);
        }
        if (sourceValue instanceof Integer) {
            return (int) sourceValue < Integer.parseInt(targetStr);
        }
        if (sourceValue instanceof Long) {
            return (long) sourceValue < Long.parseLong(targetStr);
        }
        return false;
    }
}