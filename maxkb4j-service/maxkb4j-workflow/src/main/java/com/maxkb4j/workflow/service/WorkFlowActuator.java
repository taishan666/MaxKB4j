package com.maxkb4j.workflow.service;

import com.maxkb4j.workflow.handler.ChatWorkflowHandler;
import com.maxkb4j.workflow.handler.KnowledgeWorkflowHandler;
import com.maxkb4j.workflow.model.KnowledgeWorkflow;
import com.maxkb4j.workflow.model.Workflow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import service.IWorkFlowActuator;

@RequiredArgsConstructor
@Component
public class WorkFlowActuator implements IWorkFlowActuator {

    private final ChatWorkflowHandler chatWorkflowHandler;
    private final KnowledgeWorkflowHandler knowledgeWorkflowHandler;

    public void execute(Workflow workflow) {
      if (workflow instanceof KnowledgeWorkflow) {
            knowledgeWorkflowHandler.execute(workflow);
        }else {
          chatWorkflowHandler.execute(workflow);
      }
    }

}
