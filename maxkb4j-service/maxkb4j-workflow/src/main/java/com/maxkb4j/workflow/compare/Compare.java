package com.maxkb4j.workflow.compare;

public interface Compare {

    boolean support(String compare);

    boolean compare(Object sourceValue,String targetValue);
}
