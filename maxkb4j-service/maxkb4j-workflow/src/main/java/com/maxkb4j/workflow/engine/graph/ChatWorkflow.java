package com.maxkb4j.workflow.engine.graph;

import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.workflow.engine.WorkflowExecutionAccessor;
import com.maxkb4j.workflow.engine.WorkflowOutputManager;
import lombok.Getter;

/**
 * 聊天（应用）工作流
 * 由 {@link ChatWorkflowBuilder} 构建，构建完成后状态不可变。
 */
@Getter
public class ChatWorkflow extends AbstractWorkflow {

    /**
     * 聊天参数
     */
    private final ChatParams chatParams;

    /**
     * 对话执行上下文（服务端解析的身份信息与历史记录）
     */
    private final ChatState chatState;

    ChatWorkflow(ChatWorkflowBuilder builder) {
        super(compose(builder));
        this.chatParams = builder.chatParams;
        this.chatState = builder.chatState;
        // 加载节点状态（恢复执行）
        if (builder.restoreState) {
            this.executionAccessor.loadNodeState(this, builder.details,
                    builder.currentNodeId, builder.currentNodeData);
        }
    }

    /**
     * 组装聊天工作流组件束
     */
    private static Components compose(ChatWorkflowBuilder builder) {
        WorkflowExecutionAccessor executionAccessor = new WorkflowExecutionAccessor(
                builder.configuration, builder.context, builder.navigator);
        WorkflowOutputManager outputManager = new WorkflowOutputManager(
                builder.configuration, builder.context, builder.sink);
        return new Components(builder.configuration, builder.context, builder.historyManager,
                executionAccessor, outputManager);
    }

}
