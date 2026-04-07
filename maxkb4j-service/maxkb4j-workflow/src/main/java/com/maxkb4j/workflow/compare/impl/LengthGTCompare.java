package com.maxkb4j.workflow.compare.impl;


import com.maxkb4j.workflow.annotation.CompareType;
import com.maxkb4j.workflow.compare.AbstractLengthCompare;
import com.maxkb4j.workflow.enums.CompareOperator;
import org.springframework.stereotype.Component;

@Component
@CompareType(CompareOperator.LENGTH_GT)
public class LengthGTCompare extends AbstractLengthCompare {

    @Override
    protected boolean compareLength(int sourceLen, int targetLen) {
        return sourceLen > targetLen;
    }
}