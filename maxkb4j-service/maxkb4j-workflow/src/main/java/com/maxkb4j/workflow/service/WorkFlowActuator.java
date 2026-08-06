package com.maxkb4j.workflow.service;

import com.maxkb4j.workflow.model.IWorkflow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Workflow actuator that delegates execution to appropriate handlers.
 * Uses strategy pattern to select the correct handler based on workflow type.
 */
@RequiredArgsConstructor
@Component
public class WorkFlowActuator implements IWorkFlowActuator {

    private final List<IWorkflowHandler> handlers;

    @Override
    public void execute(IWorkflow workflow) {
        handlers.stream()
                .filter(handler -> handler.canHandle(workflow))
                .findFirst()
                .ifPresentOrElse(
                        handler -> handler.execute(workflow),
                        () -> {
                            throw new IllegalStateException(
                                    "No handler found for workflow type: " + workflow.getClass().getSimpleName());
                        }
                );
    }

}
