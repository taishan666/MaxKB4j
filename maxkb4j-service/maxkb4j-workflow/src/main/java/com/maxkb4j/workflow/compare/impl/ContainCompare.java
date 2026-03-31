package com.maxkb4j.workflow.compare.impl;


import com.maxkb4j.workflow.annotation.CompareType;
import com.maxkb4j.workflow.compare.Compare;
import com.maxkb4j.workflow.enums.CompareOperator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@CompareType(CompareOperator.CONTAIN)
public class ContainCompare implements Compare {

    @Override
    public boolean compare(Object sourceValue, Object targetValue) {
        if (sourceValue == null) {
            return false;
        }
        if (sourceValue instanceof List<?>) {
            return ((List<?>) sourceValue).contains(targetValue);
        }
        if (sourceValue instanceof String && targetValue instanceof String) {
            return ((String) sourceValue).contains((String) targetValue);
        }
        return false;
    }
}