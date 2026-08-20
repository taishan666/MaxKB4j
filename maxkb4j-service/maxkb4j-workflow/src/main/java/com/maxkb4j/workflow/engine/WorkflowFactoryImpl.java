package com.maxkb4j.workflow.engine;

import com.maxkb4j.workflow.enums.WorkflowMode;
import com.maxkb4j.workflow.engine.graph.ChatLoopWorkflow;
import com.maxkb4j.workflow.engine.graph.ChatWorkflow;
import com.maxkb4j.workflow.engine.graph.ChatWorkflowBuilder;
import com.maxkb4j.workflow.engine.graph.KnowledgeLoopWorkflow;
import com.maxkb4j.workflow.engine.graph.KnowledgeWorkflow;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.service.WorkflowFactory;
import com.maxkb4j.workflow.service.WorkflowSpec;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 工作流工厂实现
 * <p>
 * 所有工作流变体（应用/知识库/循环）的唯一装配点：
 * <ul>
 *   <li>外部模块（application / knowledge）通过 {@link WorkflowSpec} 规格统一构造，隔离引擎实现细节</li>
 *   <li>引擎内部（循环节点处理器）同样经本工厂派生循环子工作流，
 *       Chat/Knowledge 循环变体的差异被收敛在此处</li>
 * </ul>
 * 具体图类的 instanceof 判断仅存在于本工厂内——这正是工厂的职责边界：
 * 新增变体时只需扩展此处与 {@link WorkflowSpec.Kind}，调用方零改动。</p>
 */
@Component
public class WorkflowFactoryImpl implements WorkflowFactory {

    @Override
    public IWorkflow create(WorkflowSpec spec) {
        Objects.requireNonNull(spec, "spec cannot be null");
        switch (spec.getKind()) {
            case APPLICATION:
                return createApplication(spec);
            case KNOWLEDGE:
                return createKnowledge(spec);
            case LOOP:
                return createLoop(spec);
            default:
                throw new IllegalArgumentException("Unsupported workflow kind: " + spec.getKind());
        }
    }

    private IWorkflow createApplication(WorkflowSpec spec) {
        return ChatWorkflowBuilder.create(WorkflowMode.APPLICATION, spec.getNodes(), spec.getEdges())
                .chatParams(spec.getChatParams())
                .chatState(spec.getChatState())
                .sink(spec.getSink())
                .build();
    }

    private IWorkflow createKnowledge(WorkflowSpec spec) {
        return new KnowledgeWorkflow(spec.getNodes(), spec.getEdges(), spec.getKnowledgeParams());
    }

    /**
     * 派生循环子工作流：依据父工作流的具体类型选择对应变体。
     */
    private IWorkflow createLoop(WorkflowSpec spec) {
        IWorkflow parent = spec.getParent();
        if (parent instanceof ChatWorkflow chatParent) {
            return new ChatLoopWorkflow(chatParent, spec.getNodes(), spec.getEdges(),
                    spec.getLoopParams(), spec.getDetails(), spec.getSink());
        }
        if (parent instanceof KnowledgeWorkflow knowledgeParent) {
            return new KnowledgeLoopWorkflow(knowledgeParent, spec.getNodes(), spec.getEdges(), spec.getLoopParams());
        }
        throw new IllegalArgumentException(
                "Unsupported loop parent workflow: " + (parent != null ? parent.getClass().getName() : "null"));
    }
}
