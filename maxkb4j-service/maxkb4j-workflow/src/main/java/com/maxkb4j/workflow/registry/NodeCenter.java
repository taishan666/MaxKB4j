package com.maxkb4j.workflow.registry;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.factory.NodeRegistry;
import com.maxkb4j.workflow.handler.node.INodeHandler;
import com.maxkb4j.workflow.logic.LfNode;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.*;
import com.maxkb4j.workflow.service.INodeCreator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 统一节点中心
 * 作为 Spring Bean 统一管理节点创建和处理器注册
 *
 * 解决的问题：
 * - 消除双重注册机制（NodeFactory + NodeHandlerBuilder）
 * - 消除静态变量（NodeBuilder、NodeHandlerBuilder）
 * - 提供统一的节点管理入口
 */
@Slf4j
@Component
public class NodeCenter implements INodeCreator {

    /**
     * 节点创建函数注册表（来自API层的NodeRegistry）
     */
    @Getter
    private final NodeRegistry nodeRegistry;

    /**
     * 处理器注册表（委托给 NodeHandlerRegistry）
     */
    private final NodeHandlerRegistry handlerRegistry;

    public NodeCenter(NodeRegistry nodeRegistry, NodeHandlerRegistry handlerRegistry) {
        this.nodeRegistry = nodeRegistry;
        this.handlerRegistry = handlerRegistry;
        registerDefaultNodeCreators();
        log.info("NodeCenter initialized with {} node creators", nodeRegistry.size());
    }

    /**
     * 注册默认节点创建函数
     * 从 NodeFactory 迁移的逻辑
     */
    private void registerDefaultNodeCreators() {
        register(NodeType.BASE, BaseNode::new);
        register(NodeType.START, StartNode::new);
        register(NodeType.AI_CHAT, AiChatNode::new);
        register(NodeType.SEARCH_KNOWLEDGE, SearchKnowledgeNode::new);
        register(NodeType.CONDITION, ConditionNode::new);
        register(NodeType.REPLY, DirectReplyNode::new);
        register(NodeType.APPLICATION, ApplicationNode::new);
        register(NodeType.QUESTION, QuestionNode::new);
        register(NodeType.IMAGE_GENERATE, ImageGenerateNode::new);
        register(NodeType.TEXT_TO_SPEECH, TextToSpeechNode::new);
        register(NodeType.DOCUMENT_EXTRACT, DocumentExtractNode::new);
        register(NodeType.DOCUMENT_SPLIT, DocumentSpiltNode::new);
        register(NodeType.SPEECH_TO_TEXT, SpeechToTextNode::new);
        register(NodeType.VARIABLE_ASSIGN, VariableAssignNode::new);
        register(NodeType.VARIABLE_AGGREGATE, VariableAggregationNode::new);
        register(NodeType.VARIABLE_SPLITTING, VariableSplittingNode::new);
        register(NodeType.TOOL, ToolNode::new);
        register(NodeType.TOOL_LIB, ToolLibNode::new);
        register(NodeType.IMAGE_UNDERSTAND, ImageUnderstandNode::new);
        register(NodeType.RERANKER, RerankerNode::new);
        register(NodeType.FORM, FormNode::new);
        register(NodeType.LOOP_BREAK, LoopBreakNode::new);
        register(NodeType.LOOP_CONTINUE, LoopContinueNode::new);
        register(NodeType.LOOP_START, LoopStartNode::new);
        register(NodeType.LOOP, LoopNode::new);
        register(NodeType.MCP, McpNode::new);
        register(NodeType.NL2SQL, NL2SqlNode::new);
        register(NodeType.INTENT_CLASSIFY, IntentClassifyNode::new);
        register(NodeType.HTTP_CLIENT, HttpNode::new);
        register(NodeType.PARAMETER_EXTRACTION, ParameterExtractionNode::new);
        register(NodeType.USER_SELECT, UserSelectNode::new);
        register(NodeType.DATA_SOURCE_LOCAL, DataSourceLocalNode::new);
        register(NodeType.DATA_SOURCE_WEB, DataSourceWebNode::new);
        register(NodeType.KNOWLEDGE_WRITE, KnowledgeWriteNode::new);
    }

    /**
     * 注册节点创建函数
     *
     * @param nodeType 节点类型
     * @param creator  节点创建函数
     */
    public void register(NodeType nodeType, NodeRegistry.NodeCreator creator) {
        nodeRegistry.register(nodeType.getKey(), creator);
    }

    /**
     * 创建节点实例
     * 实现 INodeCreator 接口
     *
     * @param lfNode 前端节点数据
     * @return 节点实例
     */
    @Override
    public AbsNode createNode(LfNode lfNode) {
        if (lfNode == null) {
            log.error("LfNode 不能为 null");
            return null;
        }
        String type = lfNode.getType();
        NodeRegistry.NodeCreator creator = nodeRegistry.getCreator(type);
        if (creator == null) {
            log.error("不支持的节点类型: {}", type);
            return null;
        }
        Object created = creator.create(lfNode.getId(), lfNode.getProperties());
        return (AbsNode) created;
    }

    /**
     * 获取处理器
     * 委托给 NodeHandlerRegistry
     *
     * @param nodeType 节点类型标识
     * @return 处理器实例
     * @throws IllegalStateException 如果处理器不存在
     */
    public INodeHandler getHandler(String nodeType) {
        return handlerRegistry.get(nodeType);
    }

    /**
     * 注册处理器
     * 委托给 NodeHandlerRegistry
     *
     * @param nodeType 节点类型标识
     * @param handler  处理器实例
     * @return true 如果该类型已有处理器被覆盖
     */
    public boolean registerHandler(String nodeType, INodeHandler handler) {
        return handlerRegistry.register(nodeType, handler);
    }

    /**
     * 检查是否已注册指定节点类型的处理器
     * 委托给 NodeHandlerRegistry
     *
     * @param nodeType 节点类型标识
     * @return 是否已注册
     */
    public boolean hasHandler(String nodeType) {
        return handlerRegistry.has(nodeType);
    }

    /**
     * 获取已注册的处理器数量
     * 委托给 NodeHandlerRegistry
     *
     * @return 处理器数量
     */
    public int handlerCount() {
        return handlerRegistry.size();
    }

}