package com.maxkb4j.workflow.compare.impl;


import com.maxkb4j.workflow.annotation.CompareType;
import com.maxkb4j.workflow.compare.Compare;
import com.maxkb4j.workflow.enums.CompareOperator;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@CompareType(CompareOperator.NOT_CONTAIN)
public class NotContainCompare implements Compare {

    @Override
    public boolean compare(Object sourceValue, String targetValue) {
        if (sourceValue == null) {
            return true;
        }
        if (sourceValue instanceof List<?>) {
            return !((List<?>) sourceValue).contains(targetValue);
        }
        if (sourceValue instanceof String) {
            return !((String) sourceValue).contains(targetValue);
        }
        return true;
    }
}