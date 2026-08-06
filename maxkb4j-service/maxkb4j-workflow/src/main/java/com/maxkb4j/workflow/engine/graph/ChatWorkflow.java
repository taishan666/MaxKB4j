package com.maxkb4j.workflow.engine.graph;

import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.workflow.engine.WorkflowExecutionAccessor;
import com.maxkb4j.workflow.engine.WorkflowOutputManager;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatWorkflow extends WorkflowImpl {

    /**
     * 聊天参数
     * -- SETTER --
     * 设置聊天参数
     */
    private ChatParams chatParams;

    /**
     * 对话执行上下文（服务端解析的身份信息与历史记录）
     * -- SETTER --
     * 设置对话执行上下文
     */
    private ChatState chatState;


    ChatWorkflow(ChatWorkflowBuilder builder) {
        // 调用父类保护构造器
        super();
        this.chatParams = builder.chatParams;
        this.chatState = builder.chatState;
        // 1. 基础组件（从 builder 获取）
        this.configuration = builder.configuration;
        this.workflowContext = builder.context;
        this.historyManager = builder.historyManager;

        this.executionAccessor = new WorkflowExecutionAccessor(
                this.configuration, this.workflowContext, builder.navigator);
        this.outputManager = new WorkflowOutputManager(
                this.configuration, this.workflowContext, builder.sink);
        // 3. 加载节点状态（恢复执行）
        if (builder.restoreState) {
            this.executionAccessor.loadNodeState(this, builder.details,
                    builder.currentNodeId, builder.currentNodeData);
        }
    }

}
