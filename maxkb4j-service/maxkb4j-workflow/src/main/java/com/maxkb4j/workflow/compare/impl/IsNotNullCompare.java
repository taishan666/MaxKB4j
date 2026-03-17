package com.maxkb4j.workflow.compare.impl;


import com.maxkb4j.workflow.annotation.CompareType;
import com.maxkb4j.workflow.compare.Compare;
import com.maxkb4j.workflow.enums.CompareOperator;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Objects;

@Component
@CompareType(CompareOperator.IS_NOT_NULL)
public class IsNotNullCompare implements Compare {

    @Override
    public boolean compare(Object sourceValue, String targetValue) {
        if (sourceValue instanceof Collection) {
            return !CollectionUtils.isEmpty((Collection<?>) sourceValue);
        } else {
            return Objects.nonNull(sourceValue) && !"".equals(sourceValue);
        }
    }
}