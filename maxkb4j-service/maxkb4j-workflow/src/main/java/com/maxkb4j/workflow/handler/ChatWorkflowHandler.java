package com.maxkb4j.workflow.handler;

import com.maxkb4j.common.domain.vo.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.exception.ExceptionResolverChain;
import com.maxkb4j.workflow.model.IChatWorkflow;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.registry.NodeCenter;
import com.maxkb4j.workflow.util.NodeChatMessageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handler for chat workflows (chat and chat-loop): streams node lifecycle events
 * as chat messages to the workflow sink.
 */
@Slf4j
@Component
public class ChatWorkflowHandler extends AbsWorkflowHandler<IChatWorkflow> {

    public ChatWorkflowHandler(NodeCenter nodeCenter,
                               ExceptionResolverChain exceptionResolverChain) {
        super(IChatWorkflow.class, nodeCenter, exceptionResolverChain);
    }

    @Override
    public void onNodeStart(IChatWorkflow workflow, AbsNode node) {
        emit(workflow, node, "", false);
        if(NodeType.LOOP.getKey().equals(node.getType())){
            emit(workflow, node, "", true);
        }
    }

    @Override
    public void onNodeSuccess(IChatWorkflow workflow, AbsNode node, NodeResult result) {
        emit(workflow, node, node.getAnswerText(), true);
    }

    @Override
    public void onNodeError(IChatWorkflow workflow, AbsNode node, Exception ex) {
        super.onNodeError(workflow, node, ex);
        emit(workflow, node, String.format("Exception: %s", ex.getMessage()), true);
    }

    /**
     * Wraps the node state into a chat message and emits it to the workflow sink.
     */
    private void emit(IChatWorkflow workflow, AbsNode node, String content, boolean nodeIsEnd) {
        ChatParams chatParams = workflow.getChatParams();
        ChatMessageVO message = NodeChatMessageUtil.buildChatMessage(
                chatParams,
                content,
                "",
                node,
                null,
                nodeIsEnd);
        workflow.output().emit(message);
    }
}
