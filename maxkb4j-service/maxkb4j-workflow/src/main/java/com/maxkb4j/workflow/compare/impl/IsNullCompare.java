package com.maxkb4j.workflow.compare.impl;


import com.maxkb4j.workflow.annotation.CompareType;
import com.maxkb4j.workflow.compare.Compare;
import com.maxkb4j.workflow.enums.CompareOperator;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Objects;

@Component
@CompareType(CompareOperator.IS_NULL)
public class IsNullCompare implements Compare {

    @Override
    public boolean compare(Object sourceValue, String targetValue) {
        if (sourceValue instanceof Collection) {
            return CollectionUtils.isEmpty((Collection<?>) sourceValue);
        } else {
            return Objects.isNull(sourceValue) || "".equals(sourceValue);
        }
    }
}