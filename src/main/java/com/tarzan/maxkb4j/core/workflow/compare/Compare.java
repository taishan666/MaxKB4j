package com.tarzan.maxkb4j.core.workflow.compare;

public interface Compare {

    boolean support(String compare);

    boolean compare(Object sourceValue,String targetValue);
}
