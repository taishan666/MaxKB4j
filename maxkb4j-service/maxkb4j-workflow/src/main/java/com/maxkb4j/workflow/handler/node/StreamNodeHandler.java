package com.maxkb4j.workflow.handler.node;

import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.vo.ChatMessageVO;
import com.maxkb4j.common.domain.vo.ChildNode;
import com.maxkb4j.workflow.model.IChatWorkflow;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.maxkb4j.workflow.enums.NodeType.FORM;
import static com.maxkb4j.workflow.enums.NodeType.USER_SELECT;

/**
 * 子类仍需自行实现 {@link #doExecuteAsync}（构建 Assistant 并启动流）、
 */
@Slf4j
public abstract class StreamNodeHandler extends AbsNodeHandler {

    @Override
    protected NodeResult doExecute(IWorkflow workflow, AbsNode node) throws Exception {
        throw new UnsupportedOperationException("Streaming node uses async execution via doExecuteAsync");
    }

    // ==================== 流式消息发送 ====================

    /**
     * 构造 {@link ChatMessageVO} 并通过工作流输出流发送。
     *
     * @param workflow  工作流上下文
     * @param node      节点实例
     * @param content   消息内容
     * @param reasoning 推理内容
     */
    protected void emitMessage(IWorkflow workflow, AbsNode node, String content, String reasoning) {
        if (workflow instanceof IChatWorkflow chatWorkflow) {
            ChatParams chatParams = chatWorkflow.getChatParams();
            ChatMessageVO vo = node.toChatMessageVO(
                    chatParams.getChatId(),
                    chatParams.getChatRecordId(),
                    node.getNodeName(),
                    content,
                    reasoning,
                    null,
                    false
            );
            workflow.output().emit(vo);
        }
    }

    protected void emitMessage(ChatMessageVO message,String chatId,String chatRecordId,AtomicBoolean isInterruptExec, IWorkflow workflow, AbsNode node) {
        if (FORM.getKey().equals(message.getNodeType()) || USER_SELECT.getKey().equals(message.getNodeType())) {
            isInterruptExec.set(StringUtils.isNotEmpty(message.getContent()));
        }
        ChildNode childNode = new ChildNode(message.getChatRecordId(), message.getRuntimeNodeId());
        ChatMessageVO vo = node.toChatMessageVO(
                chatId,
                chatRecordId,
                node.getNodeName(),
                message.getContent(),
                message.getReasoningContent(),
                childNode,
                message.getNodeIsEnd());
        vo.setNodeType(message.getNodeType());
        vo.setViewType(message.getViewType());
        workflow.output().emit(vo);
    }

}
