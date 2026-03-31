package com.maxkb4j.workflow.compare.impl;


import com.maxkb4j.workflow.annotation.CompareType;
import com.maxkb4j.workflow.compare.Compare;
import com.maxkb4j.workflow.enums.CompareOperator;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@CompareType(CompareOperator.LENGTH_LE)
public class LengthLECompare implements Compare {

    @Override
    public boolean compare(Object sourceValue, Object targetValue) {
        if (sourceValue == null || targetValue == null) {
            return false;
        }
        int target = Integer.parseInt(targetValue.toString());
        if (sourceValue instanceof List<?>) {
            return ((List<?>) sourceValue).size() <= target;
        }
        if (sourceValue instanceof String) {
            return ((String) sourceValue).length() <= target;
        }
        return false;
    }
}