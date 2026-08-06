package com.maxkb4j.workflow.engine;

import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.workflow.enums.WorkflowMode;
import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Sinks;

import java.util.Collections;
import java.util.List;

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


    public ChatWorkflow(List<AbsNode> nodes, List<LfEdge> edges, ChatParams chatParams, ChatState chatState, Sinks.Many<ChatMessageVO> sink) {
        // 调用父类保护构造器
        super();
        this.chatParams = chatParams;
        this.chatState = chatState;
        // 1. 初始化配置
        super.configuration = new WorkflowConfiguration(WorkflowMode.APPLICATION, nodes, edges);

        // 2. 初始化上下文
        super.workflowContext = new WorkflowContext();

        // 3. 初始化历史管理器
        super.historyManager = new HistoryManager(Collections.emptyList());

        // 4. 初始化执行控制器
        super.executionAccessor = new WorkflowExecutionAccessor(super.configuration, super.workflowContext, new EdgeNavigator(edges));

        // 5. 初始化输出管理器
        super.outputManager = new WorkflowOutputManager(super.configuration, super.workflowContext, sink);
    }


}
