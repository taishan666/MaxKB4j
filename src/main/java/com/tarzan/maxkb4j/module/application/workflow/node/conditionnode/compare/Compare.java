package com.tarzan.maxkb4j.module.application.workflow.node.conditionnode.compare;

public interface Compare {

    boolean support(String compare);

    boolean compare(Object sourceValue,String targetValue);
}
