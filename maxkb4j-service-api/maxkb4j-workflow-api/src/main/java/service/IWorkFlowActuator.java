package service;

import com.maxkb4j.workflow.model.Workflow;

public interface IWorkFlowActuator {
    void execute(Workflow workflow);
}
