package com.maxkb4j.workflow.engine.graph;

import com.maxkb4j.workflow.model.LoopParams;

import java.util.Map;

public interface ILoopWorkFlow {

    LoopParams getLoopParams();

    Map<String, Object> getLoopContext();
}
