package com.maxkb4j.workflow.compare.impl;


import com.maxkb4j.workflow.annotation.CompareType;
import com.maxkb4j.workflow.compare.Compare;
import com.maxkb4j.workflow.enums.CompareOperator;
import org.springframework.stereotype.Component;

@Component
@CompareType(CompareOperator.IS_NOT_TRUE)
public class IsNotTrueCompare implements Compare {

    @Override
    public boolean compare(Object sourceValue, Object targetValue) {
        if (sourceValue == null) {
            return true;
        }
        if (sourceValue instanceof Boolean) {
            return !(Boolean) sourceValue;
        } else if (sourceValue instanceof String) {
            return !Boolean.parseBoolean((String) sourceValue);
        }
        return true;
    }
}