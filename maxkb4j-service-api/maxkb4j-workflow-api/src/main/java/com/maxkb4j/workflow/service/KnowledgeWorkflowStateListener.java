package com.maxkb4j.workflow.service;

import com.alibaba.fastjson.JSONObject;

/**
 * 知识库系工作流状态监听器
 *
 * <p>由 knowledge 模块提供实现并注册为 Spring Bean，workflow 引擎在
 * 知识库工作流生命周期节点（启动/完成）变更时回调本接口，
 * 从而反转 workflow → knowledge 的依赖方向：workflow 只依赖本契约，
 * 不感知 knowledge 模块的具体服务。</p>
 */
public interface KnowledgeWorkflowStateListener {

    /**
     * 工作流状态变更回调
     *
     * @param actionId      知识库动作 ID
     * @param runtimeDetails 各节点运行时详情
     * @param state         目标状态（ActionStatus 名称）
     */
    void onStateChange(String actionId, JSONObject runtimeDetails, String state);
}
