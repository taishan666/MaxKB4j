package com.maxkb4j.workflow.engine.graph;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatRecordDTO;
import com.maxkb4j.workflow.engine.*;
import com.maxkb4j.workflow.enums.WorkflowMode;
import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.node.AbsNode;
import reactor.core.publisher.Sinks.Many;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ChatWorkflow 构建器
 * 分离复杂初始化逻辑，提供清晰的构建流程
 * <p>
 * 设计原则：
 * - 必需参数通过构造器传入
 * - 可选参数通过链式方法设置
 * - 组件初始化顺序在 build() 中统一管理
 * <p>
 * 使用示例：
 * <pre>
 * ChatWorkflow workflow = ChatWorkflowBuilder.create(mode, nodes, edges)
 *     .chatParams(chatParams)
 *     .chatState(chatState)
 *     .sink(sink)
 *     .restoreState(details, nodeId, nodeData)
 *     .build();
 * </pre>
 */
public class ChatWorkflowBuilder {

    // ==================== 必需参数 ====================
    private final WorkflowMode workflowMode;
    private final List<AbsNode> nodes;
    private final List<LfEdge> edges;
    // ==================== 可选参数 ====================
    ChatParams chatParams;
    ChatState chatState;
    Many<ChatMessageVO> sink;
    JSONObject details;
    String currentNodeId;
    Map<String, Object> currentNodeData;
    boolean restoreState = false;
    // ==================== 内部构建的组件（供 ChatWorkflow 构造器使用） ====================
    WorkflowConfiguration configuration;
    WorkflowContext context;
    HistoryManager historyManager;
    EdgeNavigator navigator;

    /**
     * 构造器（必需参数）
     *
     * @param mode  工作流模式
     * @param nodes 节点列表
     * @param edges 边列表
     */
    public ChatWorkflowBuilder(WorkflowMode mode, List<AbsNode> nodes, List<LfEdge> edges) {
        this.workflowMode = Objects.requireNonNull(mode, "workflowMode cannot be null");
        this.nodes = nodes != null ? nodes : Collections.emptyList();
        this.edges = edges != null ? edges : Collections.emptyList();
    }

    // ==================== 可选参数设置方法 ====================

    /**
     * 设置聊天参数
     *
     * @param params 聊天参数
     * @return this
     */
    public ChatWorkflowBuilder chatParams(ChatParams params) {
        this.chatParams = params;
        return this;
    }

    /**
     * 设置对话执行上下文（服务端解析的身份信息与历史记录）。
     *
     * @param chatState 对话执行上下文
     * @return this
     */
    public ChatWorkflowBuilder chatState(ChatState chatState) {
        this.chatState = chatState;
        return this;
    }

    /**
     * 设置响应式输出 Sink
     *
     * @param sink Sink 实例
     * @return this
     */
    public ChatWorkflowBuilder sink(Many<ChatMessageVO> sink) {
        this.sink = sink;
        return this;
    }

    /**
     * 设置待恢复的执行状态
     *
     * @param details  节点详情
     * @param nodeId   当前节点运行时 ID
     * @param nodeData 当前节点数据
     * @return this
     */
    public ChatWorkflowBuilder restoreState(JSONObject details, String nodeId, Map<String, Object> nodeData) {
        this.details = details;
        this.currentNodeId = nodeId;
        this.currentNodeData = nodeData;
        this.restoreState = (details != null && nodeId != null);
        return this;
    }

    // ==================== 构建方法 ====================

    /**
     * 构建 ChatWorkflow 实例
     *
     * @return ChatWorkflow 实例
     */
    public ChatWorkflow build() {
        // 1. 构建 Configuration
        this.configuration = new WorkflowConfiguration(workflowMode, nodes, edges);
        // 2. 构建 Context
        this.context = new WorkflowContext();
        // 3. 恢复执行状态：chatRecord 来自上下文，runtimeNodeId/nodeData 来自请求入参
        if (chatParams != null
                && chatState != null
                && chatState.getChatRecord() != null
                && chatState.getChatRecord().getDetails() != null) {
            restoreState(chatState.getChatRecord().getDetails(),
                    chatParams.getRuntimeNodeId(), chatParams.getNodeData());
        }
        // 4. 构建 HistoryManager（历史记录来自上下文）
        List<ChatRecordDTO> history = chatState != null
                ? chatState.getHistoryChatRecords()
                : Collections.emptyList();
        this.historyManager = new HistoryManager(history);
        // 5. 构建 Navigator
        this.navigator = new EdgeNavigator(edges);
        // 6. 构建 Workflow（内部完成依赖组件初始化）
        return new ChatWorkflow(this);
    }

    /**
     * 创建构建器
     *
     * @param mode  工作流模式
     * @param nodes 节点列表
     * @param edges 边列表
     * @return ChatWorkflowBuilder 实例
     */
    public static ChatWorkflowBuilder create(WorkflowMode mode, List<AbsNode> nodes, List<LfEdge> edges) {
        return new ChatWorkflowBuilder(mode, nodes, edges);
    }

}
