package com.maxkb4j.workflow.engine.graph;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.model.IChatWorkflow;
import com.maxkb4j.workflow.model.LoopParams;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Sinks;

import java.util.List;

/**
 * 聊天场景的循环工作流
 * 继承 {@link AbstractLoopWorkflow} 的公共循环构建逻辑（含 loopParams），
 * 同时持有父工作流的聊天参数与对话执行上下文，
 * 支持从上次执行详情（details）恢复节点状态。
 */
@Slf4j
@Getter
public class ChatLoopWorkflow extends AbstractLoopWorkflow implements IChatWorkflow {

    /**
     * 聊天参数（来自父工作流）
     */
    private final ChatParams chatParams;

    /**
     * 对话执行上下文（服务端解析的身份信息与历史记录，来自父工作流）
     */
    private final ChatState chatState;

    /**
     * 构造器（使用父工作流上下文）
     *
     * @param parent     父工作流
     * @param nodes      循环内节点列表
     * @param edges      循环内边列表
     * @param loopParams 循环参数
     * @param details    上次执行的节点详情（可为 null）
     * @param sink       输出 Sink
     */
    public ChatLoopWorkflow(ChatWorkflow parent, List<AbsNode> nodes, List<LfEdge> edges,
                            LoopParams loopParams, JSONObject details, Sinks.Many<ChatMessageVO> sink) {
        super(composeLoopComponents(parent, nodes, edges, sink), loopParams);
        this.chatParams = parent.getChatParams();
        this.chatState = parent.getChatState();
        restoreNodeState(details);
    }

    /**
     * 从上次执行详情恢复节点状态
     * details 为空或缺少聊天参数时跳过恢复（防御 NPE）
     */
    private void restoreNodeState(JSONObject details) {
        if (details == null || details.isEmpty()) {
            return;
        }
        if (chatParams == null) {
            log.warn("Skip restoring loop node state: chatParams is null");
            return;
        }
        this.executionAccessor.loadNodeState(this, details,
                chatParams.getRuntimeNodeId(), chatParams.getNodeData());
    }
}
