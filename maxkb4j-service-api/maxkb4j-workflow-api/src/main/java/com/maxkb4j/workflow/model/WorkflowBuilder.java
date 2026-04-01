package com.maxkb4j.workflow.model;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatRecordDTO;
import com.maxkb4j.workflow.enums.WorkflowMode;
import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.node.AbsNode;
import reactor.core.publisher.Sinks.Many;
import com.maxkb4j.common.domain.dto.ChatMessageVO;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Workflow 构建器
 * 分离复杂初始化逻辑，提供清晰的构建流程
 * <p>
 * 设计原则：
 * - 必需参数通过构造器传入
 * - 可选参数通过链式方法设置
 * - 组件初始化顺序在 build() 中统一管理
 * <p>
 * 使用示例：
 * <pre>
 * Workflow workflow = WorkflowBuilder.create(mode, nodes, edges)
 *     .chatParams(chatParams)
 *     .sink(sink)
 *     .restoreState(details, nodeId, nodeData)
 *     .build();
 * </pre>
 */
public class WorkflowBuilder {

    // ==================== 必需参数 ====================
    private final WorkflowMode workflowMode;
    private final List<AbsNode> nodes;
    private final List<LfEdge> edges;
    // ==================== 可选参数 ====================
    ChatParams chatParams;
    Many<ChatMessageVO> sink;
    JSONObject details;
    String currentNodeId;
    Map<String, Object> currentNodeData;
    Map<String, Object> loopContext;
    boolean restoreState = false;
    // ==================== 内部构建的组件（供 Workflow 构造器使用） ====================
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
    public WorkflowBuilder(WorkflowMode mode, List<AbsNode> nodes, List<LfEdge> edges) {
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
    public WorkflowBuilder chatParams(ChatParams params) {
        this.chatParams = params;
        if (params != null &&
            params.getChatRecord() != null &&
           params.getChatRecord().getDetails() != null
        ) {
            restoreState(chatParams.getChatRecord().getDetails(), chatParams.getRuntimeNodeId(), chatParams.getNodeData());
        }
        return this;
    }

    /**
     * 设置响应式输出 Sink
     *
     * @param sink Sink 实例
     * @return this
     */
    public WorkflowBuilder sink(Many<ChatMessageVO> sink) {
        this.sink = sink;
        return this;
    }

    public void restoreState(JSONObject details, String nodeId, Map<String, Object> nodeData) {
        this.details = details;
        this.currentNodeId = nodeId;
        this.currentNodeData = nodeData;
        this.restoreState = (details != null && nodeId != null);
    }

    // ==================== 构建方法 ====================

    /**
     * 构建 Workflow 实例
     *
     * @return Workflow 实例
     */
    public Workflow build() {
        // 1. 构建 Configuration
        this.configuration = new WorkflowConfiguration(workflowMode, nodes, edges);
        this.configuration.setChatParams(chatParams);
        // 2. 构建 Context
        this.context = new WorkflowContext();
        // 3. 构建 HistoryManager
        List<ChatRecordDTO> history = chatParams != null
                ? chatParams.getHistoryChatRecords()
                : Collections.emptyList();
        this.historyManager = new HistoryManager(history);
        // 4. 构建 Navigator
        this.navigator = new EdgeNavigator(edges);
        // 5. 构建 Workflow（内部完成依赖组件初始化）
        return new Workflow(this);
    }

    /**
     * 创建构建器
     *
     * @param mode  工作流模式
     * @param nodes 节点列表
     * @param edges 边列表
     * @return WorkflowBuilder 实例
     */
    public static WorkflowBuilder create(WorkflowMode mode, List<AbsNode> nodes, List<LfEdge> edges) {
        return new WorkflowBuilder(mode, nodes, edges);
    }

}