package com.maxkb4j.workflow.handler;

import com.maxkb4j.workflow.enums.ActionStatus;
import com.maxkb4j.workflow.exception.ExceptionResolverChain;
import com.maxkb4j.workflow.model.IKnowledgeWorkflow;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.registry.NodeCenter;
import com.maxkb4j.workflow.service.KnowledgeWorkflowStateListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.Executor;
import static com.maxkb4j.workflow.consts.WorkflowConstants.BeanName;

@Slf4j
@Component
public class KnowledgeWorkflowHandler extends AbsWorkflowHandler {

    private final Optional<KnowledgeWorkflowStateListener> stateListener;

    public KnowledgeWorkflowHandler(NodeCenter nodeCenter,
                                    @Qualifier(BeanName.WORKFLOW_TASK_EXECUTOR) Executor workflowTaskExecutor,
                                    ExceptionResolverChain exceptionResolverChain,
                                    Optional<KnowledgeWorkflowStateListener> stateListener) {
        super(nodeCenter, workflowTaskExecutor, exceptionResolverChain);
        this.stateListener = stateListener;
    }

    @Override
    public boolean canHandle(IWorkflow workflow) {
        // KnowledgeWorkflowHandler 处理所有知识库系工作流（KnowledgeWorkflow 与知识库循环工作流）
        return (workflow instanceof IKnowledgeWorkflow);
    }

    @Override
    protected void onNodeStart(IWorkflow workflow, AbsNode node) {
        updateState(workflow, ActionStatus.STARTED);
    }

    @Override
    protected void onProcessCompleted(IWorkflow workflow) {
        updateState(workflow, ActionStatus.SUCCESS);
    }

    /**
     * 按接口 {@link IKnowledgeWorkflow} 判断（与 {@link #canHandle} 保持一致），
     * 确保知识库循环工作流（仅实现接口、不继承具体类）的状态更新同样生效。
     */
    private void updateState(IWorkflow workflow, ActionStatus actionStatus) {
        if (workflow instanceof IKnowledgeWorkflow knowledgeWorkflow) {
            String actionId = knowledgeWorkflow.getKnowledgeParams().getActionId();
            stateListener.ifPresent(listener ->
                    listener.onStateChange(actionId, knowledgeWorkflow.output().runtimeDetails(), actionStatus.name()));
        }
    }
}
