package com.maxkb4j.core.workflow.handler;

import com.maxkb4j.core.workflow.builder.NodeHandlerBuilder;
import com.maxkb4j.core.workflow.enums.ActionStatus;
import com.maxkb4j.core.workflow.enums.NodeStatus;
import com.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.maxkb4j.core.workflow.model.KnowledgeWorkflow;
import com.maxkb4j.core.workflow.model.NodeResult;
import com.maxkb4j.core.workflow.model.NodeResultFuture;
import com.maxkb4j.core.workflow.model.Workflow;
import com.maxkb4j.core.workflow.node.AbsNode;
import com.maxkb4j.knowledge.service.IKnowledgeActionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeWorkflowHandler extends WorkflowHandler {

    private final IKnowledgeActionService knowledgeActionService;

    @Override
    public void execute(Workflow workflow) {
        if (workflow instanceof KnowledgeWorkflow knowledgeWorkflow) {
            updateState(knowledgeWorkflow, ActionStatus.SUCCESS);
        }
    }

    public void updateState(Workflow workflow, ActionStatus actionStatus) {
        if (workflow instanceof KnowledgeWorkflow knowledgeWorkflow) {
            String actionId = knowledgeWorkflow.getKnowledgeParams().getActionId();
            knowledgeActionService.updateState(actionId, knowledgeWorkflow.getRuntimeDetails(), actionStatus.name());
        }
    }


    @Override
    public NodeResultFuture runNodeFuture(Workflow workflow, AbsNode node) {
        try {
            long startTime = System.currentTimeMillis();
            node.setStatus(NodeStatus.STARTED.getStatus());
            updateState(workflow, ActionStatus.STARTED);
            INodeHandler nodeHandler = NodeHandlerBuilder.getHandler(node.getType());
            NodeResult result = nodeHandler.execute(workflow, node);
            float runTime = (System.currentTimeMillis() - startTime) / 1000F;
            node.getDetail().put("runTime", runTime);
            log.info("node:{}, runTime:{} s", node.getProperties().getString("nodeName"), runTime);
            return new NodeResultFuture(result, null, NodeStatus.SUCCESS.getStatus());
        } catch (Exception ex) {
            log.error("error:", ex);
            node.setErrMessage(ex.getMessage());
            log.error("NODE: {} Exception :{}", node.getType(), ex.getMessage());
            return new NodeResultFuture(null, ex, NodeStatus.ERROR.getStatus());
        }
    }
}
