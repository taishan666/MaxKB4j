package com.tarzan.maxkb4j.core.workflow.handler;

import com.tarzan.maxkb4j.core.workflow.builder.NodeHandlerBuilder;
import com.tarzan.maxkb4j.core.workflow.enums.ActionStatus;
import com.tarzan.maxkb4j.core.workflow.enums.NodeRunStatus;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.KnowledgeWorkflow;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.core.workflow.model.NodeResultFuture;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.INode;
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
            knowledgeActionService.updateState(workflow, ActionStatus.SUCCESS);
        }
        return workflow.getAnswer();
    }


    @Override
    public NodeResultFuture runNodeFuture(Workflow workflow, INode node) {
        try {
            long startTime = System.currentTimeMillis();
            node.setStatus(202);
            knowledgeActionService.updateState(workflow, ActionStatus.STARTED);
            INodeHandler nodeHandler = NodeHandlerBuilder.getHandler(node.getType());
            NodeResult result = nodeHandler.execute(workflow, node);
            float runTime = (System.currentTimeMillis() - startTime) / 1000F;
            node.getDetail().put("runTime", runTime);
            log.info("node:{}, runTime:{} s", node.getProperties().getString("nodeName"), runTime);
            node.setRunStatus(NodeRunStatus.SUCCESS);
            return new NodeResultFuture(result, null, 200);
        } catch (Exception ex) {
            log.error("error:", ex);
            node.setErrMessage(ex.getMessage());
            log.error("NODE: {} Exception :{}", node.getType(), ex.getMessage());
            node.setRunStatus(NodeRunStatus.ERROR);
            return new NodeResultFuture(null, ex, 500);
        }
    }
}
