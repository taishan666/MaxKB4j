package com.maxkb4j.workflow.handler;

import com.maxkb4j.workflow.enums.ActionStatus;
import com.maxkb4j.workflow.exception.ExceptionResolverChain;
import com.maxkb4j.workflow.model.IKnowledgeWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.registry.NodeCenter;
import com.maxkb4j.workflow.service.KnowledgeWorkflowStateListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Handler for knowledge workflows (knowledge and knowledge-loop): reports workflow
 * state changes to the optional {@link KnowledgeWorkflowStateListener}.
 */
@Slf4j
@Component
public class KnowledgeWorkflowHandler extends AbsWorkflowHandler<IKnowledgeWorkflow> {

    private final Optional<KnowledgeWorkflowStateListener> stateListener;

    public KnowledgeWorkflowHandler(NodeCenter nodeCenter,
                                    ExceptionResolverChain exceptionResolverChain,
                                    Optional<KnowledgeWorkflowStateListener> stateListener) {
        super(IKnowledgeWorkflow.class, nodeCenter, exceptionResolverChain);
        this.stateListener = stateListener;
    }

    @Override
    public void onNodeStart(IKnowledgeWorkflow workflow, AbsNode node) {
        updateState(workflow, ActionStatus.STARTED);
    }

    @Override
    protected void onProcessCompleted(IKnowledgeWorkflow workflow) {
        updateState(workflow, ActionStatus.SUCCESS);
    }

    private void updateState(IKnowledgeWorkflow workflow, ActionStatus actionStatus) {
        String actionId = workflow.getKnowledgeParams().getActionId();
        stateListener.ifPresent(listener ->
                listener.onStateChange(actionId, workflow.output().runtimeDetails(), actionStatus.name()));
    }
}
