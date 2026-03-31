package com.maxkb4j.workflow.handler;

import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.workflow.model.KnowledgeWorkflow;
import com.maxkb4j.workflow.model.NodeResultFuture;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.registry.NodeCenter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class ChatWorkflowHandler extends AbsWorkflowHandler {

    public ChatWorkflowHandler(NodeCenter nodeCenter,
                               @Qualifier("workflowExecutor") Executor workflowExecutor) {
        super(nodeCenter, workflowExecutor);
    }

    @Override
    public boolean canHandle(Workflow workflow) {
        // ChatWorkflowHandler handles all workflows except KnowledgeWorkflow
        return !(workflow instanceof KnowledgeWorkflow);
    }

    @Override
    protected NodeResultFuture handleNodeError(Workflow workflow, AbsNode node, Exception ex) {
        NodeResultFuture result = super.handleNodeError(workflow, node, ex);
        emitErrorToSink(workflow, node, ex);
        return result;
    }

    /**
     * Sends an error message to the workflow's sink if applicable.
     *
     * @param workflow the workflow context
     * @param node     the node that failed
     * @param ex       the exception that occurred
     */
    protected void emitErrorToSink(Workflow workflow, AbsNode node, Exception ex) {
        if (workflow.getChatParams() != null && workflow.needsSinkOutput()) {
            ChatMessageVO errMessage = node.toChatMessageVO(
                    workflow.getChatParams().getChatId(),
                    workflow.getChatParams().getChatRecordId(),
                    String.format("Exception: %s", ex.getMessage()),
                    "",
                    null,
                    true);
            workflow.getSink().tryEmitNext(errMessage);
        }
    }
}