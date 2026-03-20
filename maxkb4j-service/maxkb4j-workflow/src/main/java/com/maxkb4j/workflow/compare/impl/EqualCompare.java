package com.maxkb4j.workflow.compare.impl;


import com.maxkb4j.workflow.annotation.CompareType;
import com.maxkb4j.workflow.compare.Compare;
import com.maxkb4j.workflow.enums.CompareOperator;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@CompareType(CompareOperator.EQ)
public class EqualCompare implements Compare {

    @Override
    public boolean compare(Object sourceValue, String targetValue) {
        return Objects.equals(sourceValue, targetValue);
    }
}