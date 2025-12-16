package com.tarzan.maxkb4j.core.workflow.handler;

import com.tarzan.maxkb4j.core.workflow.model.KnowledgeWorkflow;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.KnowledgeActionEntity;
import com.tarzan.maxkb4j.module.knowledge.service.KnowledgeActionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class KnowledgeWorkflowHandler extends WorkflowHandler {

    private final KnowledgeActionService knowledgeActionService;

    public KnowledgeWorkflowHandler(TaskExecutor chatTaskExecutor, KnowledgeActionService knowledgeActionService) {
        super(chatTaskExecutor);
        this.knowledgeActionService = knowledgeActionService;
    }

    @Override
    public String execute(Workflow workflow) {
        if (workflow instanceof KnowledgeWorkflow knowledgeWorkflow) {
            List<INode> nodes = knowledgeWorkflow.getStartNodes();
            runChainNodes(workflow, nodes);
            KnowledgeActionEntity knowledgeActionEntity = new KnowledgeActionEntity();
            knowledgeActionEntity.setId(knowledgeWorkflow.getKnowledgeParams().getActionId());
            knowledgeActionEntity.setDetails(workflow.getRuntimeDetails());
            knowledgeActionEntity.setState("SUCCESS");
            knowledgeActionService.updateById(knowledgeActionEntity);
        }
        return workflow.getAnswer();
    }

    @Override
    public void addExecutedNode(Workflow workflow, INode node) {
        workflow.appendNode(node);
        if (workflow instanceof KnowledgeWorkflow knowledgeWorkflow) {
            KnowledgeActionEntity knowledgeActionEntity = new KnowledgeActionEntity();
            knowledgeActionEntity.setId(knowledgeWorkflow.getKnowledgeParams().getActionId());
            knowledgeActionEntity.setDetails(workflow.getRuntimeDetails());
            knowledgeActionService.updateById(knowledgeActionEntity);
        }
    }
}
