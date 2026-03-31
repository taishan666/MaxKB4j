package com.maxkb4j.workflow.model;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatRecordDTO;
import com.maxkb4j.workflow.enums.WorkflowMode;
import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.node.AbsNode;
import dev.langchain4j.data.message.ChatMessage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Sinks;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 工作流协调器
 * 聚合工作流各组件，提供统一的访问入口
 *
 * 重构后职责：
 * - 聚合 WorkflowConfiguration（配置管理）
 * - 聚合 WorkflowContext（上下文管理）
 * - 聚合 WorkflowExecutionController（执行控制）
 * - 聚合 WorkflowOutputManager（输出管理）
 * - 聚合 HistoryManager（历史消息）
 * - 聚合 VariableResolver（变量解析）
 * - 聚合 TemplateRenderer（模板渲染）
 */
@Slf4j
@Getter
@Setter
public class Workflow {

    // ==================== 配置组件 ====================
    protected WorkflowConfiguration configuration;

    // ==================== 上下文组件 ====================
    protected WorkflowContext workflowContext;
    protected HistoryManager historyManager;
    protected VariableResolver variableResolver;
    protected TemplateRenderer templateRenderer;

    // ==================== 执行控制组件 ====================
    protected WorkflowExecutionController executionController;

    // ==================== 输出管理组件 ====================
    protected WorkflowOutputManager outputManager;

    // ==================== 边导航器 ====================
    protected EdgeNavigator edgeNavigator;

    // ==================== 构造器 ====================

    public Workflow(WorkflowMode workflowMode, List<AbsNode> nodes, List<LfEdge> edges) {
        this(workflowMode, nodes, edges, ChatParams.builder().build(),
             Sinks.many().unicast().onBackpressureBuffer(), null);
    }

    public Workflow(WorkflowMode workflowMode, List<AbsNode> nodes, List<LfEdge> edges,
                    ChatParams chatParams, Sinks.Many<com.maxkb4j.common.domain.dto.ChatMessageVO> sink) {
        this(workflowMode, nodes, edges, chatParams, sink,
             chatParams != null && chatParams.getChatRecord() != null
                ? chatParams.getChatRecord().getDetails()
                : null);
    }

    public Workflow(WorkflowMode workflowMode, List<AbsNode> nodes, List<LfEdge> edges,
                    ChatParams chatParams, JSONObject details,
                    Sinks.Many<com.maxkb4j.common.domain.dto.ChatMessageVO> sink) {
        // 1. 初始化配置
        this.configuration = new WorkflowConfiguration(workflowMode, nodes, edges);
        this.configuration.setChatParams(chatParams);

        // 2. 初始化上下文组件
        this.workflowContext = new WorkflowContext();
        this.historyManager = new HistoryManager(
                chatParams != null ? chatParams.getHistoryChatRecords() : Collections.emptyList());
        this.variableResolver = new VariableResolver(this.workflowContext);
        this.templateRenderer = new TemplateRenderer(this.variableResolver);

        // 3. 初始化边导航器
        this.edgeNavigator = new EdgeNavigator(edges);

        // 4. 初始化执行控制器
        this.executionController = new WorkflowExecutionController(
                this.configuration, this.workflowContext, this.edgeNavigator, this.templateRenderer);

        // 5. 初始化输出管理器
        this.outputManager = new WorkflowOutputManager(
                this.configuration, this.workflowContext, sink);

        // 6. 加载节点状态（恢复执行）
        if (chatParams != null && StringUtils.isNotBlank(chatParams.getRuntimeNodeId())
                && Objects.nonNull(chatParams.getChatRecord()) && details != null) {
            this.executionController.loadNodeState(this, details,
                    chatParams.getRuntimeNodeId(), chatParams.getNodeData());
        }
    }

    // ==================== 便捷访问方法 ====================

    /**
     * 获取全局上下文
     */
    public Map<String, Object> getContext() {
        return workflowContext.getGlobalContext();
    }

    /**
     * 获取聊天上下文
     */
    public Map<String, Object> getChatContext() {
        return workflowContext.getChatContext();
    }

    /**
     * 获取历史聊天记录
     */
    public List<ChatRecordDTO> getHistoryChatRecords() {
        return historyManager.historyChatRecords();
    }

    /**
     * 获取历史消息
     */
    public List<ChatMessage> getHistoryMessages(int dialogueNumber, String dialogueType, String runtimeNodeId) {
        return historyManager.getHistoryMessages(dialogueNumber, dialogueType, runtimeNodeId);
    }

    /**
     * 渲染提示词模板
     */
    public String renderPrompt(String prompt) {
        return templateRenderer.render(prompt);
    }

    /**
     * 获取引用字段值
     */
    public Object getReferenceField(List<String> reference) {
        if (CollectionUtils.isNotEmpty(reference) && reference.size() > 1) {
            return variableResolver.getReferenceField(reference.get(0), reference.get(1));
        }
        return null;
    }

    /**
     * 获取字段值
     */
    @SuppressWarnings("unchecked")
    public Object getFieldValue(Object value, String source) {
        if ("reference".equals(source) && value instanceof List) {
            List<String> fields = (List<String>) value;
            return variableResolver.getReferenceField(fields.get(0), fields.get(1));
        }
        return value;
    }

    /**
     * 根据节点ID获取节点
     */
    public AbsNode getNode(String nodeId) {
        return configuration.getNode(nodeId);
    }
}