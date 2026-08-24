package com.maxkb4j.knowledge.listener;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.knowledge.service.IKnowledgeActionInternalService;
import com.maxkb4j.workflow.service.KnowledgeWorkflowStateListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 知识库工作流状态监听器实现
 *
 * <p>作为 workflow-api 中 {@link KnowledgeWorkflowStateListener} 契约的适配器，
 * 将 workflow 引擎的状态变更回调委托给知识库动作服务持久化，
 * 使 workflow 模块无需依赖 knowledge 模块（依赖倒置）。</p>
 */
@Component
@RequiredArgsConstructor
public class KnowledgeWorkflowStateListenerImpl implements KnowledgeWorkflowStateListener {

    private final IKnowledgeActionInternalService knowledgeActionService;

    @Override
    public void onStateChange(String actionId, JSONObject runtimeDetails, String state) {
        knowledgeActionService.updateState(actionId, runtimeDetails, state);
    }
}
