package com.maxkb4j.workflow.engine;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.maxkb4j.workflow.consts.WorkflowConstants.FormField;
import static com.maxkb4j.workflow.consts.WorkflowConstants.RuntimeDetailField;

/**
 * 节点状态加载器
 * <p>
 * 负责节点的运行时实例化（按配置解析节点并注入上游列表等运行时信息）与
 * 执行状态恢复（从上次执行的节点详情重建上下文与当前节点）。
 * </p>
 */
@Slf4j
public class NodeStateLoader {

    /**
     * 工作流配置（节点解析来源）
     */
    private final WorkflowConfiguration configuration;
    /**
     * 工作流上下文（恢复的节点追加到上下文）
     */
    private final WorkflowContext context;

    public NodeStateLoader(WorkflowConfiguration configuration, WorkflowContext context) {
        this.configuration = configuration;
        this.context = context;
    }

    /**
     * 加载节点状态
     * 用于恢复中断的工作流执行
     *
     * @param workflow        工作流实例（用于 saveContext）
     * @param details         节点详情
     * @param currentNodeId   当前节点运行时ID
     * @param currentNodeData 当前节点数据
     * @return 恢复出的当前节点，未命中或参数非法时返回 null
     */
    @SuppressWarnings("unchecked")
    public AbsNode loadNodeState(IWorkflow workflow, JSONObject details, String currentNodeId, Map<String, Object> currentNodeData) {
        if (details == null || currentNodeId == null) {
            log.warn("loadNodeState called with null details or currentNodeId");
            return null;
        }
        configuration.getNodes().forEach(e->e.setStatus(NodeStatus.SKIP.getStatus()));
        List<Map<String, Object>> sortedDetails = details.values().stream()
                .filter(Objects::nonNull)
                .map(row -> (Map<String, Object>) row)
                .sorted(Comparator.comparing(
                        e -> (Integer) e.get(RuntimeDetailField.INDEX),
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .toList();
        AbsNode restoredCurrentNode = null;
        for (Map<String, Object> nodeDetail : sortedDetails) {
            String nodeId = (String) nodeDetail.get(RuntimeDetailField.NODE_ID);
            List<String> upNodeIdList = (List<String>) nodeDetail.get(RuntimeDetailField.UP_NODE_ID_LIST);
            String runtimeNodeId = (String) nodeDetail.get(RuntimeDetailField.RUNTIME_NODE_ID);
            Integer nodeStatus = (Integer) nodeDetail.get(RuntimeDetailField.STATUS);
            if (Objects.equals(runtimeNodeId, currentNodeId)) {
                // 处理当前节点
                restoredCurrentNode = configuration.getNodeInstance(nodeId, upNodeIdList, n -> {
                    JSONObject nodeProperties = n.getProperties();
                    if (nodeProperties.containsKey(RuntimeDetailField.NODE_DATA)) {
                        JSONObject nodeData = nodeProperties.getJSONObject(RuntimeDetailField.NODE_DATA);
                        nodeData.put(FormField.FORM_DATA, currentNodeData);
                    }
                    return nodeProperties;
                });
                if (restoredCurrentNode != null) {
                    restoredCurrentNode.setStatus(NodeStatus.READY.getStatus());
                    restoredCurrentNode.saveContext(workflow, nodeDetail);
                    restoredCurrentNode.setDetail(nodeDetail);
                    context.appendNode(restoredCurrentNode);
                }
            } else {
                // 处理其他节点
                AbsNode node = configuration.getNodeInstance(nodeId, upNodeIdList, null);
                if (node != null) {
                    node.setStatus(nodeStatus);
                    node.saveContext(workflow, nodeDetail);
                    node.setDetail(nodeDetail);
                    context.appendNode(node);
                }
            }
        }
        return restoredCurrentNode;
    }
}
