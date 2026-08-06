package com.maxkb4j.workflow.engine.graph;

import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.model.KnowledgeParams;
import com.maxkb4j.workflow.model.LoopParams;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Getter;

import java.util.List;

/**
 * 知识库场景的循环工作流
 * 继承 {@link AbstractLoopWorkflow} 的公共循环构建逻辑（含 loopParams），
 * 同时持有父工作流的知识库参数，无 Sink 输出。
 */
@Getter
public class KnowledgeLoopWorkflow extends AbstractLoopWorkflow {

    /**
     * 知识库参数（来自父工作流）
     */
    private final KnowledgeParams knowledgeParams;

    /**
     * 构造器（使用父工作流上下文）
     *
     * @param parent     父工作流
     * @param nodes      循环内节点列表
     * @param edges      循环内边列表
     * @param loopParams 循环参数
     */
    public KnowledgeLoopWorkflow(KnowledgeWorkflow parent, List<AbsNode> nodes, List<LfEdge> edges, LoopParams loopParams) {
        super(composeLoopComponents(parent, nodes, edges, null), loopParams);
        this.knowledgeParams = parent.getKnowledgeParams();
    }
}
