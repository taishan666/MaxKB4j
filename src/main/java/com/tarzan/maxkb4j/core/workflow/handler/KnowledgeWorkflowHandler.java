package com.tarzan.maxkb4j.core.workflow.handler;

import com.tarzan.maxkb4j.core.workflow.builder.NodeHandlerBuilder;
import com.tarzan.maxkb4j.core.workflow.enums.ActionStatus;
import com.tarzan.maxkb4j.core.workflow.enums.NodeStatus;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.KnowledgeWorkflow;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.core.workflow.model.NodeResultFuture;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.module.knowledge.service.KnowledgeActionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeWorkflowHandler extends WorkflowHandler {

    private final KnowledgeActionService knowledgeActionService;

    @Override
    public void execute(Workflow workflow) {
        if (workflow instanceof KnowledgeWorkflow knowledgeWorkflow) {
            List<AbsNode> nodes = knowledgeWorkflow.getStartNodes();
            runChainNodes(workflow, nodes);
            knowledgeActionService.updateState(workflow, ActionStatus.SUCCESS);
        }
    }


    @Override
    public NodeResultFuture runNodeFuture(Workflow workflow, AbsNode node) {
        try {
            long startTime = System.currentTimeMillis();
            node.setStatus(NodeStatus.STARTED.getStatus());
            knowledgeActionService.updateState(workflow, ActionStatus.STARTED);
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
