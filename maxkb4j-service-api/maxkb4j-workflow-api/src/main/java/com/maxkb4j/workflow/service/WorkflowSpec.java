package com.maxkb4j.workflow.service;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.KnowledgeParams;
import com.maxkb4j.workflow.model.LoopParams;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Getter;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Objects;

/**
 * 工作流构建规格（不可变值对象）
 *
 * <p>作为 {@link WorkflowFactory#create(WorkflowSpec)} 的统一入参，
 * 以"规格 + 参数"模式取代按业务域硬编码的工厂方法：
 * <ul>
 *   <li>{@link Kind#APPLICATION}：应用（聊天）工作流</li>
 *   <li>{@link Kind#KNOWLEDGE}：知识库工作流</li>
 *   <li>{@link Kind#LOOP}：循环子工作流（依据父工作流派生变体）</li>
 * </ul>
 * 调用方通过类型化静态工厂（{@link #application}/{@link #knowledge}/{@link #loop}）
 * 获得预置了 kind 与必填项的 Builder，避免传入与 kind 无关的字段。</p>
 */
@Getter
public final class WorkflowSpec {

    /** 工作流规格类别 */
    public enum Kind {
        APPLICATION, KNOWLEDGE, LOOP
    }

    private final Kind kind;
    private final List<AbsNode> nodes;
    private final List<LfEdge> edges;
    // ---- APPLICATION ----
    private final ChatParams chatParams;
    private final ChatState chatState;
    private final Sinks.Many<ChatMessageVO> sink;
    // ---- KNOWLEDGE ----
    private final KnowledgeParams knowledgeParams;
    // ---- LOOP ----
    private final IWorkflow parent;
    private final LoopParams loopParams;
    private final JSONObject details;

    private WorkflowSpec(Builder builder) {
        this.kind = builder.kind;
        this.nodes = builder.nodes;
        this.edges = builder.edges;
        this.chatParams = builder.chatParams;
        this.chatState = builder.chatState;
        this.sink = builder.sink;
        this.knowledgeParams = builder.knowledgeParams;
        this.parent = builder.parent;
        this.loopParams = builder.loopParams;
        this.details = builder.details;
    }

    /** 应用（聊天）工作流规格 */
    public static Builder application(List<AbsNode> nodes, List<LfEdge> edges) {
        return new Builder(Kind.APPLICATION, nodes, edges);
    }

    /** 知识库工作流规格 */
    public static Builder knowledge(List<AbsNode> nodes, List<LfEdge> edges, KnowledgeParams knowledgeParams) {
        return new Builder(Kind.KNOWLEDGE, nodes, edges).knowledgeParams(knowledgeParams);
    }

    /** 循环子工作流规格（变体由父工作流决定） */
    public static Builder loop(IWorkflow parent, List<AbsNode> nodes, List<LfEdge> edges, LoopParams loopParams) {
        return new Builder(Kind.LOOP, nodes, edges).parent(parent).loopParams(loopParams);
    }

    /**
     * 规格构建器：静态工厂已按 kind 预置必填项，build() 时做终态校验。
     */
    public static final class Builder {

        private final Kind kind;
        private final List<AbsNode> nodes;
        private final List<LfEdge> edges;
        private KnowledgeParams knowledgeParams;
        private IWorkflow parent;
        private LoopParams loopParams;
        private ChatParams chatParams;
        private ChatState chatState;
        private Sinks.Many<ChatMessageVO> sink;
        private JSONObject details;

        private Builder(Kind kind, List<AbsNode> nodes, List<LfEdge> edges) {
            this.kind = Objects.requireNonNull(kind, "kind cannot be null");
            this.nodes = Objects.requireNonNull(nodes, "nodes cannot be null");
            this.edges = Objects.requireNonNull(edges, "edges cannot be null");
        }

        public Builder chatParams(ChatParams chatParams) {
            this.chatParams = chatParams;
            return this;
        }

        public Builder chatState(ChatState chatState) {
            this.chatState = chatState;
            return this;
        }

        public Builder sink(Sinks.Many<ChatMessageVO> sink) {
            this.sink = sink;
            return this;
        }

        public Builder details(JSONObject details) {
            this.details = details;
            return this;
        }

        private Builder knowledgeParams(KnowledgeParams knowledgeParams) {
            this.knowledgeParams = knowledgeParams;
            return this;
        }

        private Builder parent(IWorkflow parent) {
            this.parent = parent;
            return this;
        }

        private Builder loopParams(LoopParams loopParams) {
            this.loopParams = loopParams;
            return this;
        }

        public WorkflowSpec build() {
            switch (kind) {
                case KNOWLEDGE:
                    Objects.requireNonNull(knowledgeParams, "knowledgeParams is required for KNOWLEDGE workflow");
                    break;
                case LOOP:
                    Objects.requireNonNull(parent, "parent workflow is required for LOOP workflow");
                    Objects.requireNonNull(loopParams, "loopParams is required for LOOP workflow");
                    break;
                default:
                    break;
            }
            return new WorkflowSpec(this);
        }
    }
}
