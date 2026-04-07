package com.maxkb4j.workflow.compare.impl;


import com.maxkb4j.workflow.annotation.CompareType;
import com.maxkb4j.workflow.compare.AbstractNumericCompare;
import com.maxkb4j.workflow.enums.CompareOperator;
import org.springframework.stereotype.Component;

@Component
@CompareType(CompareOperator.LE)
public class LECompare extends AbstractNumericCompare {

    @Override
    protected boolean compareNumeric(double source, double target) {
        return source <= target;
    }
}