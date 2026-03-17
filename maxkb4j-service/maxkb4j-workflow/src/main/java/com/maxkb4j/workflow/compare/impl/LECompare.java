package com.maxkb4j.workflow.compare.impl;


import com.maxkb4j.workflow.annotation.CompareType;
import com.maxkb4j.workflow.compare.Compare;
import com.maxkb4j.workflow.enums.CompareOperator;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@CompareType(CompareOperator.LE)
public class LECompare implements Compare {

    @Override
    public boolean compare(Object sourceValue, String targetValue) {
        if (sourceValue == null || targetValue == null) {
            return false;
        }
        if (sourceValue instanceof Collection) {
            return ((Collection<?>) sourceValue).size() <= Integer.parseInt(targetValue);
        }
        if (sourceValue instanceof Float) {
            return (float) sourceValue <= Float.parseFloat(targetValue);
        }
        if (sourceValue instanceof Double) {
            return (double) sourceValue <= Double.parseDouble(targetValue);
        }
        if (sourceValue instanceof Integer) {
            return (int) sourceValue <= Integer.parseInt(targetValue);
        }
        if (sourceValue instanceof Long) {
            return (long) sourceValue <= Long.parseLong(targetValue);
        }
        return false;
    }
}