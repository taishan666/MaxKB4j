package com.maxkb4j.workflow.handler;

import com.maxkb4j.knowledge.service.IKnowledgeActionService;
import com.maxkb4j.workflow.engine.graph.KnowledgeWorkflow;
import com.maxkb4j.workflow.enums.ActionStatus;
import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.exception.ExceptionResolverChain;
import com.maxkb4j.workflow.model.IKnowledgeWorkflow;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.registry.NodeCenter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Slf4j
@Component
public class KnowledgeWorkflowHandler extends AbsWorkflowHandler {

    private final IKnowledgeActionService knowledgeActionService;

    public KnowledgeWorkflowHandler(NodeCenter nodeCenter,
                                    @Qualifier("workflowTaskExecutor") Executor workflowTaskExecutor,
                                    ExceptionResolverChain exceptionResolverChain,
                                    IKnowledgeActionService knowledgeActionService) {
        super(nodeCenter, workflowTaskExecutor, exceptionResolverChain);
        this.knowledgeActionService = knowledgeActionService;
    }

    @Override
    public boolean canHandle(IWorkflow workflow) {
        // KnowledgeWorkflowHandler 处理所有知识库系工作流（KnowledgeWorkflow 与知识库循环工作流）
        return (workflow instanceof IKnowledgeWorkflow);
    }

    @Override
    protected void onNodeStart(IWorkflow workflow, AbsNode node) {
        updateState(workflow, ActionStatus.STARTED);
    }

    @Override
    protected void onProcessCompleted(IWorkflow workflow) {
        updateState(workflow, ActionStatus.SUCCESS);
    }

    public void updateState(IWorkflow workflow, ActionStatus actionStatus) {
        if (workflow instanceof KnowledgeWorkflow knowledgeWorkflow) {
            String actionId = knowledgeWorkflow.getKnowledgeParams().getActionId();
            knowledgeActionService.updateState(actionId, knowledgeWorkflow.output().runtimeDetails(), actionStatus.name());
        }
    }
}
