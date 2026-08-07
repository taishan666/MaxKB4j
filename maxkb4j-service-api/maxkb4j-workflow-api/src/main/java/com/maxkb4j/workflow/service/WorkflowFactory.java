package com.maxkb4j.workflow.service;

import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.model.KnowledgeParams;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import reactor.core.publisher.Sinks;

import java.util.List;

/**
 * 工作流工厂 SPI（契约层）。
 * <p>
 * 供外部模块（application / knowledge）构造工作流，避免依赖 workflow 实现模块的具体引擎类。
 * 实现位于 workflow 实现模块。
 */
public interface WorkflowFactory {

    /**
     * 构造应用（聊天）工作流，恢复执行状态并接入响应式输出。
     *
     * @param nodes        节点列表
     * @param edges        边列表
     * @param chatParams   聊天请求入参（含恢复状态所需的 runtimeNodeId/nodeData）
     * @param chatState  对话执行上下文（含历史记录与身份信息）
     * @param sink         响应式输出 Sink
     * @return 工作流实例
     */
    IWorkflow createApplication(List<AbsNode> nodes, List<LfEdge> edges, ChatParams chatParams, ChatState chatState, Sinks.Many<ChatMessageVO> sink);

    /**
     * 构造知识库工作流。
     *
     * @param nodes           节点列表
     * @param edges            边列表
     * @param knowledgeParams  知识库参数
     * @return 工作流实例
     */
    IWorkflow createKnowledge(List<AbsNode> nodes, List<LfEdge> edges, KnowledgeParams knowledgeParams);
}
