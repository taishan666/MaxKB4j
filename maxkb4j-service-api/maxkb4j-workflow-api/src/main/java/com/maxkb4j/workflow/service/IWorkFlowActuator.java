package com.maxkb4j.workflow.service;

import com.maxkb4j.workflow.model.IWorkflow;

public interface IWorkFlowActuator {
    void execute(IWorkflow workflow);
}
