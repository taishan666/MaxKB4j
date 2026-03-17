package com.maxkb4j.workflow.handler;

import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.workflow.model.NodeResultFuture;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ChatWorkflowHandler extends AbsWorkflowHandler {

    @Override
    public void execute(Workflow workflow) {
        AbsNode currentNode = workflow.getCurrentNode();
        if (currentNode == null) {
            currentNode = workflow.getStartNode();
        }
        log.info("工作流-开始");
        runChainNodes(workflow, List.of(currentNode));
        log.info("工作流-结束");
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



