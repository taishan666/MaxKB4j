package com.tarzan.maxkb4j.core.workflow.compare.impl;


import com.tarzan.maxkb4j.core.workflow.compare.Compare;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Objects;

public class IsNotNullCompare implements Compare {
    @Override
    public boolean support(String compare) {
        return compare.equals("is_not_null");
    }

    @Override
    public boolean compare(Object sourceValue, String targetValue) {
        if (sourceValue instanceof Collection){
            return !CollectionUtils.isEmpty((Collection<?>) sourceValue);
        }else {
            return Objects.nonNull(sourceValue)&&!sourceValue.equals("");
        }
    }
}
