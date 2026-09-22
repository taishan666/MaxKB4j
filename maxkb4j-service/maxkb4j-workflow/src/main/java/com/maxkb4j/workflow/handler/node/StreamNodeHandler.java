package com.maxkb4j.workflow.handler.node;

import com.maxkb4j.common.domain.vo.ChatMessageVO;
import com.maxkb4j.common.domain.vo.ChildNode;
import com.maxkb4j.workflow.model.IChatWorkflow;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.util.NodeChatMessageUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 流式节点处理器抽象基类。
 *
 * <p>在 {@link AbsNodeHandler} 基础上，为聊天系工作流（{@link IChatWorkflow}）提供流式消息发送能力：</p>
 * <ul>
 *   <li>{@link #emitMessage} —— 以当前节点身份直接发送流式内容片段</li>
 *   <li>{@link #emitChildMessage} —— 转发子应用/子工作流产生的消息（自动包装子节点引用）</li>
 *   <li>{@link #isInterruptMessage} —— 判定消息是否为交互中断信号（表单/用户选择）</li>
 * </ul>
 *
 * <p>子类需实现 {@link #doExecuteAsync} 启动流式执行；非聊天系工作流的消息发送会被静默忽略。</p>
 */
@Slf4j
public abstract class StreamNodeHandler extends AbsNodeHandler {

    @Override
    protected NodeResult doExecute(IWorkflow workflow, AbsNode node) throws Exception {
        throw new UnsupportedOperationException("Streaming node uses async execution via doExecuteAsync");
    }

    // ==================== 流式消息发送 ====================

    /**
     * 以当前节点身份构造 {@link ChatMessageVO} 并通过工作流输出流发送。
     *
     * @param workflow 工作流上下文
     * @param node     节点实例
     * @param content  消息内容
     * @param reasoning 推理内容
     */
    protected void emitMessage(IWorkflow workflow, AbsNode node, String content, String reasoning) {
        if (workflow instanceof IChatWorkflow chatWorkflow) {
            workflow.output().emit(NodeChatMessageUtil.buildChatMessage(chatWorkflow.getChatParams(), content, reasoning, node,null, false));
        }
    }

    // ==================== 子工作流消息转发 ====================

    /**
     * 转发子应用/子工作流产生的消息到主工作流输出流：
     * 以当前节点身份重新包装消息内容，并携带子节点引用（chatRecordId + runtimeNodeId）
     * 供前端关联子会话。
     *
     * <p>本方法只负责消息转发，不产生任何副作用；交互中断的判定请使用 {@link #isInterruptMessage}。</p>
     *
     * @param message  子工作流产生的原始消息
     * @param workflow 主工作流上下文
     * @param node     当前节点实例
     */
    protected void emitChildMessage(ChatMessageVO message, IWorkflow workflow, AbsNode node) {
        if (workflow instanceof IChatWorkflow chatWorkflow) {
            ChildNode childNode = new ChildNode(message.getChatRecordId(), message.getRuntimeNodeId());
            ChatMessageVO vo = NodeChatMessageUtil.buildChatMessage(chatWorkflow.getChatParams(),
                    message.getContent(), message.getReasoningContent(),node, childNode, message.getNodeIsEnd());
            vo.setNodeType(message.getNodeType());
            vo.setViewType(message.getViewType());
            workflow.output().emit(vo);
        }
    }


}
