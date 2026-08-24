package com.maxkb4j.workflow.model;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.Answer;
import com.maxkb4j.common.domain.dto.ChatMessageVO;

import java.util.List;

/**
 * 工作流输出管理器接口（契约层）。
 * <p>
 * 定义响应式输出、答案列表、运行时详情等输出行为，供 {@link IWorkflow} 门面返回。
 * 具体实现位于 workflow 实现模块。
 */
public interface IWorkflowOutputManager {
    /**
     * 发送消息到响应式 Sink（知识库工作流不输出）。
     */
    void emit(ChatMessageVO message);
    /**
     * 获取工作流运行后所有节点回答。
     */
    List<Answer> getAnswers();
    /**
     * 获取工作流运行后节点运行时详情 JSON。
     */
    JSONObject runtimeDetails();
}
