package com.tarzan.maxkb4j.core.workflow.node.condition.compare;

public interface Compare {

    boolean support(String compare);

    boolean compare(Object sourceValue,String targetValue);
}
