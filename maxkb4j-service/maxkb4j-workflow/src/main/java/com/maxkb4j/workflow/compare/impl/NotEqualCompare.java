package com.maxkb4j.workflow.compare.impl;

import com.maxkb4j.workflow.annotation.CompareType;
import com.maxkb4j.workflow.compare.Compare;
import com.maxkb4j.workflow.enums.CompareOperator;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Not equal comparison implementation.
 * Fills the gap for CompareOperator.NE which was defined but had no implementation.
 */
@Component
@CompareType(CompareOperator.NE)
public class NotEqualCompare implements Compare {

    @Override
    public boolean compare(Object sourceValue, Object targetValue) {
        return !Objects.equals(sourceValue, targetValue);
    }
}