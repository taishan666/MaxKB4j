package com.tarzan.maxkb4j.core.workflow.factory;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.logic.LfNode;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 节点工厂
 * 使用注册表模式创建节点实例，支持动态扩展
 *
 * 相比原有的 NodeBuilder，本类：
 * - 不使用 switch 语句，符合开闭原则
 * - 新增节点类型只需注册，无需修改工厂代码
 * - 支持运行时动态注册节点类型
 */
@Slf4j
@Getter
public class NodeFactory {

    /**
     * 节点创建函数接口
     */
    @FunctionalInterface
    public interface NodeCreator {
        AbsNode create(String id, JSONObject properties);
    }

    /**
     * 节点注册表
     * -- GETTER --
     *  获取节点注册表（用于测试和扩展）
     *
     */
    private final NodeRegistry registry;

    public NodeFactory() {
        this.registry = new NodeRegistry();
        registerDefaultNodes();
    }

    /**
     * 注册默认节点类型
     */
    private void registerDefaultNodes() {
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
     * 注册节点类型
     *
     * @param nodeType 节点类型
     * @param creator  节点创建函数
     */
    public void register(NodeType nodeType, NodeCreator creator) {
        registry.register(nodeType.getKey(), creator);
    }

    /**
     * 创建节点
     *
     * @param lfNode 前端节点数据
     * @return 节点实例
     * @throws IllegalArgumentException 如果 lfNode 为 null
     * @throws IllegalStateException  如果不支持的节点类型
     */
    public AbsNode createNode(LfNode lfNode) {
        if (lfNode == null) {
            log.error("LfNode 不能为 null");
            return null;
        }
        String type = lfNode.getType();
        NodeCreator creator = registry.getCreator(type);
        if (creator == null) {
            log.error("不支持的节点类型: {}", type);
            return null;
        }
        return creator.create(lfNode.getId(), lfNode.getProperties());
    }

}
