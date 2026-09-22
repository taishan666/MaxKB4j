package com.maxkb4j.workflow.engine;

import com.maxkb4j.workflow.enums.WorkflowMode;
import com.maxkb4j.workflow.engine.graph.ChatLoopWorkflow;
import com.maxkb4j.workflow.engine.graph.ChatWorkflow;
import com.maxkb4j.workflow.engine.graph.ChatWorkflowBuilder;
import com.maxkb4j.workflow.engine.graph.KnowledgeLoopWorkflow;
import com.maxkb4j.workflow.engine.graph.KnowledgeWorkflow;
import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.WorkflowSpec;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.service.INodeCreator;
import com.maxkb4j.workflow.service.WorkflowFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
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
 * APPLICATION/KNOWLEDGE 规格直接携带 {@code LogicFlow} 解析模型，
 * LfNode 到引擎节点的转换（经 {@link INodeCreator}，含无法识别节点的过滤）收敛在本工厂。
 * 具体图类的 instanceof 判断仅存在于本工厂内——这正是工厂的职责边界：
 * 新增变体时只需扩展此处与 {@link WorkflowSpec.Kind}，调用方零改动。</p>
 */
@Component
@RequiredArgsConstructor
public class WorkflowFactoryImpl implements WorkflowFactory {

    private final INodeCreator nodeCreator;

    @Override
    public IWorkflow create(WorkflowSpec spec) {
        Objects.requireNonNull(spec, "spec cannot be null");
        return switch (spec.getKind()) {
            case APPLICATION -> createApplication(spec);
            case KNOWLEDGE -> createKnowledge(spec);
            case LOOP -> createLoop(spec);
            default -> throw new IllegalArgumentException("Unsupported workflow kind: " + spec.getKind());
        };
    }

    private IWorkflow createApplication(WorkflowSpec spec) {
        return ChatWorkflowBuilder.create(WorkflowMode.APPLICATION, engineNodes(spec), edges(spec))
                .chatParams(spec.getChatParams())
                .chatState(spec.getChatState())
                .callback(spec.getCallback())
                .build();
    }

    private IWorkflow createKnowledge(WorkflowSpec spec) {
        return new KnowledgeWorkflow(engineNodes(spec), edges(spec), spec.getKnowledgeParams());
    }

    /**
     * 派生循环子工作流：依据父工作流的具体类型选择对应变体。
     */
    private IWorkflow createLoop(WorkflowSpec spec) {
        IWorkflow parent = spec.getParent();
        if (parent instanceof ChatWorkflow chatParent) {
            return new ChatLoopWorkflow(chatParent, engineNodes(spec), edges(spec),
                    spec.getLoopParams(), spec.getDetails(), spec.getCallback());
        }
        if (parent instanceof KnowledgeWorkflow knowledgeParent) {
            return new KnowledgeLoopWorkflow(knowledgeParent, engineNodes(spec), edges(spec), spec.getLoopParams());
        }
        throw new IllegalArgumentException(
                "Unsupported loop parent workflow: " + (parent != null ? parent.getClass().getName() : "null"));
    }

    /**
     * 将规格转换为引擎节点列表：节点经 {@code INodeCreator} 创建，运行时必为 {@link AbsNode}，
     * 无法识别的节点（返回 null）被过滤。
     */
    private List<AbsNode> engineNodes(WorkflowSpec spec) {
        return spec.getLogicFlow().getNodes().stream()
                .map(nodeCreator::createNode)
                .filter(Objects::nonNull)
                .map(AbsNode.class::cast)
                .toList();
    }

    /**
     * 获取规格对应的边列表：LOOP 取派生节点配套的边，其余取 LogicFlow 解析的边。
     */
    private List<LfEdge> edges(WorkflowSpec spec) {
        return spec.getLogicFlow().getEdges();
    }
}
