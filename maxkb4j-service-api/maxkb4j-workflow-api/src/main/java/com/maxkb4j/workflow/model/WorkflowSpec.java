package com.maxkb4j.workflow.model;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.vo.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.common.domain.vo.ResultCallback;
import com.maxkb4j.workflow.logic.LogicFlow;
import com.maxkb4j.workflow.service.WorkflowFactory;
import lombok.Getter;

import java.util.Objects;

/**
 * 工作流构建规格（不可变值对象）
 *
 * <p>作为 {@link WorkflowFactory#create(WorkflowSpec)} 的统一入参，
 * 以"规格 + 参数"模式取代按业务域硬编码的工厂方法：
 * <ul>
 *   <li>{@link Kind#APPLICATION}：应用（聊天）工作流，直接由 {@link LogicFlow} 解析模型构建</li>
 *   <li>{@link Kind#KNOWLEDGE}：知识库工作流，直接由 {@link LogicFlow} 解析模型构建</li>
 *   <li>{@link Kind#LOOP}：循环子工作流（节点列表由父工作流派生，与 LogicFlow 不对应）</li>
 * </ul>
 * APPLICATION/KNOWLEDGE 的 LfNode 到引擎节点的转换由工厂实现方（经 {@code INodeCreator}）完成；
 * 调用方通过类型化静态工厂（{@link #application}/{@link #knowledge}/{@link #loop}）
 * 获得预置了 kind 与必填项的 Builder，避免传入与 kind 无关的字段。</p>
 */
@Getter
public final class WorkflowSpec {

    private final Kind kind;
    // ---- APPLICATION / KNOWLEDGE ----
    private final LogicFlow logicFlow;
    // ---- APPLICATION ----
    private final ChatParams chatParams;
    private final ChatState chatState;
    private final ResultCallback<ChatMessageVO> callback;
    // ---- KNOWLEDGE ----
    private final KnowledgeParams knowledgeParams;
    // ---- LOOP ----
    private final IWorkflow parent;
    private final LoopParams loopParams;
    private final JSONObject details;
    private WorkflowSpec(Builder builder) {
        this.kind = builder.kind;
        this.logicFlow = builder.logicFlow;
        this.chatParams = builder.chatParams;
        this.chatState = builder.chatState;
        this.callback = builder.callback;
        this.knowledgeParams = builder.knowledgeParams;
        this.parent = builder.parent;
        this.loopParams = builder.loopParams;
        this.details = builder.details;
    }

    /**
     * 应用（聊天）工作流规格
     */
    public static Builder application(LogicFlow logicFlow) {
        return new Builder(Kind.APPLICATION, logicFlow);
    }

    /**
     * 知识库工作流规格
     */
    public static Builder knowledge(LogicFlow logicFlow, KnowledgeParams knowledgeParams) {
        return new Builder(Kind.KNOWLEDGE, logicFlow).knowledgeParams(knowledgeParams);
    }

    /**
     * 循环子工作流规格（变体由父工作流决定，节点列表由调用方派生）
     */
    public static Builder loop(IWorkflow parent, LogicFlow logicFlow, LoopParams loopParams) {
        return new Builder(Kind.LOOP, logicFlow).parent(parent).loopParams(loopParams);
    }

    /**
     * 工作流规格类别
     */
    public enum Kind {
        APPLICATION, KNOWLEDGE, LOOP
    }

    /**
     * 规格构建器：静态工厂已按 kind 预置必填项，build() 时做终态校验。
     */
    public static final class Builder {

        private final Kind kind;
        private final LogicFlow logicFlow;
        private KnowledgeParams knowledgeParams;
        private IWorkflow parent;
        private LoopParams loopParams;
        private ChatParams chatParams;
        private ChatState chatState;
        private ResultCallback<ChatMessageVO> callback;
        private JSONObject details;

        private Builder(Kind kind, LogicFlow logicFlow) {
            this.kind = Objects.requireNonNull(kind, "kind cannot be null");
            this.logicFlow = Objects.requireNonNull(logicFlow, "logicFlow cannot be null");
        }


        public Builder chatParams(ChatParams chatParams) {
            this.chatParams = chatParams;
            return this;
        }

        public Builder chatState(ChatState chatState) {
            this.chatState = chatState;
            return this;
        }

        public Builder callback(ResultCallback<ChatMessageVO> callback) {
            this.callback = callback;
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
