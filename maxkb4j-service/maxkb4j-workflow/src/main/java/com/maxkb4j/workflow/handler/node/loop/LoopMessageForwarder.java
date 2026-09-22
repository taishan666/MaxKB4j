package com.maxkb4j.workflow.handler.node.loop;

import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.vo.ChatMessageVO;
import com.maxkb4j.common.domain.vo.ChildNode;
import com.maxkb4j.common.domain.vo.ResultCallback;
import com.maxkb4j.workflow.model.IChatWorkflow;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.LoopParams;
import com.maxkb4j.workflow.node.AbsNode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.maxkb4j.workflow.consts.WorkflowConstants.LoopField;
import static com.maxkb4j.workflow.enums.NodeType.*;

/**
 * 循环消息转发器
 *
 * <p>从循环节点处理器中拆出的响应式输出职责：
 * <ul>
 *   <li>为 chat 系子工作流创建 Sink 订阅，识别中断信号并转发循环内消息到主工作流</li>
 *   <li>发送迭代边界标记（迭代开始/结束），供前端渲染循环进度</li>
 * </ul>
 * 非 chat 系工作流（知识库等无输出流场景）的订阅请求返回 {@link Optional#empty()}，
 * 使迭代执行器无需感知工作流的具体类别。</p>
 */
@Component
public class LoopMessageForwarder {

    /**
     * 为 chat 系工作流创建子工作流输出订阅；非 chat 系返回 empty。
     *
     * @param workflow   主工作流
     * @param loopParams 当前迭代参数
     * @param ctx        循环执行上下文（中断标记写回）
     * @param node       循环节点（消息组装用）
     * @return 订阅句柄
     */
    Optional<ResultCallback<ChatMessageVO>> buildCallback(IWorkflow workflow, LoopParams loopParams,
                                         LoopExecutionContext ctx, AbsNode node) {
        if (!(workflow instanceof IChatWorkflow)) {
            return Optional.empty();
        }
        ResultCallback<ChatMessageVO> callback = new ResultCallback<>() {
            @Override
            public void onEvent(ChatMessageVO message) {
                if (isBreakSignal(message)) {
                    ctx.isInterrupted.set(true);
                } else {
                    handleLoopMessage(message, loopParams, ctx, workflow, node);
                }
            }

            @Override
            public void onComplete() {}

            @Override
            public void onError(Throwable e) {}
        };
        return Optional.of(callback);
    }

    /**
     * 判断是否为中断信号
     */
    private boolean isBreakSignal(ChatMessageVO message) {
        return LOOP_BREAK.getKey().equals(message.getNodeType()) && LoopField.BREAK.equals(message.getContent());
    }

    /**
     * 处理循环消息
     */
    private void handleLoopMessage(ChatMessageVO message, LoopParams loopParams, LoopExecutionContext ctx, IWorkflow workflow, AbsNode node) {
        String nodeType = message.getNodeType();
        // 表单和用户选择节点需要中断
        if (FORM.getKey().equals(nodeType) || USER_SELECT.getKey().equals(nodeType)) {
            if (StringUtils.isNotBlank(message.getContent())) {
                ctx.isInterrupted.set(true);
            }
        }
        // 更新子节点引用
        String runtimeNodeId = message.getRuntimeNodeId() + "_" + loopParams.getIndex();
        ChildNode childNode=new ChildNode(message.getChatRecordId(), runtimeNodeId);

        // 转发消息到主工作流
        emitLoopMessageVO(message, workflow, node, childNode);
    }

    /**
     * 构建循环消息VO并转发到主工作流
     */
    private void emitLoopMessageVO(ChatMessageVO message, IWorkflow workflow,
                                   AbsNode node, ChildNode childNode) {
        if (workflow instanceof IChatWorkflow chatWorkflow) {
            ChatParams chatParams = chatWorkflow.getChatParams();
            ChatMessageVO vo = node.toChatMessageVO(
                    chatParams.getChatId(),
                    chatParams.getChatRecordId(),
                    message.getNodeName(),
                    message.getContent(),
                    message.getReasoningContent(),
                    childNode,
                    message.getNodeIsEnd());
            vo.setNodeType(message.getNodeType());
            vo.setViewType(message.getViewType());
            workflow.output().emit(vo);
        }
    }

}
