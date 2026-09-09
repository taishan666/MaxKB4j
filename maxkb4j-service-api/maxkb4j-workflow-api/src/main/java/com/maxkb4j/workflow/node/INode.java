package com.maxkb4j.workflow.node;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.Answer;

import java.util.List;
import java.util.Map;

/**
 * 工作流节点契约接口（契约层）。
 * <p>
 * 定义节点的标识、配置、运行时状态与产出的读写行为，
 * 供 {@code IWorkflow}/{@code IWorkflowContext}/{@code IWorkflowExecutionAccessor}
 * 等契约引用。具体实现 {@code AbsNode} 位于 workflow 实现模块。
 */
public interface INode {

    /**
     * 节点 ID（流程图中的静态标识）。
     */
    String getId();

    /**
     * 节点类型 key（对应 NodeType）。
     */
    String getType();

    /**
     * 答案展示类型（viewType）。
     */
    String getViewType();

    /**
     * 前端节点属性。
     */
    JSONObject getProperties();

    /**
     * 节点运行时上下文（节点输出变量）。
     */
    Map<String, Object> getContext();

    /**
     * 节点运行时详情。
     */
    Map<String, Object> getDetail();

    /**
     * 上游节点 ID 列表。
     */
    List<String> getUpNodeIdList();

    /**
     * 运行时节点 ID（由节点 ID 与上游路径派生）。
     */
    String getRuntimeNodeId();

    /**
     * 节点执行状态（NodeStatus）。
     */
    Integer getStatus();

    /**
     * 设置节点执行状态（NodeStatus）。
     */
    void setStatus(Integer status);

    /**
     * 节点错误信息。
     */
    String getErrMessage();

    /**
     * 节点名称。
     */
    String getNodeName();

    /**
     * 节点数据（properties.nodeData）。
     */
    JSONObject getNodeData();

    /**
     * 获取节点回答列表（非结果节点返回空列表）。
     */
    List<Answer> getAnswerList(String chatRecordId);
}
