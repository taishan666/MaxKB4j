package com.tarzan.maxkb4j.core.workflow;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tarzan.maxkb4j.common.util.StringUtil;
import com.tarzan.maxkb4j.core.workflow.logic.LfEdge;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.core.workflow.result.NodeResultFuture;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.chat.ChatParams;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Function;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.*;

@Slf4j
@Data
public class WorkflowManage {
    private INode startNode;
    private ChatParams chatParams;
    private List<INode> nodes;
    private List<LfEdge> edges;
    private Map<String, Object> globalVariable;
    private String answer;
    private ApplicationChatRecordEntity chatRecord;
    private List<ApplicationChatRecordEntity> historyChatRecords;
    private List<INode> nodeContext;

    public WorkflowManage(List<INode> nodes, List<LfEdge> edges, ChatParams chatParams, ApplicationChatRecordEntity chatRecord, List<ApplicationChatRecordEntity> historyChatRecords) {
        this.nodes = nodes;
        this.edges = edges;
        this.chatParams = chatParams;
        this.globalVariable = new HashMap<>();
        this.nodeContext = new ArrayList<>();
        this.chatRecord = chatRecord;
        this.answer = "";
        this.historyChatRecords = CollectionUtils.isEmpty(historyChatRecords) ? List.of() : historyChatRecords;
        //todo runtimeNodeId 的作用
        if (StringUtil.isNotBlank(chatParams.getRuntimeNodeId()) && Objects.nonNull(chatRecord)) {
            this.loadNode(chatRecord, chatParams.getRuntimeNodeId(), chatParams.getNodeData());
        }

    }


    public void loadNode(ApplicationChatRecordEntity chatRecord, String startNodeId, Map<String, Object> startNodeData) {
        List<JSONObject> sortedDetails = chatRecord.getDetails().values().stream()
                .map(row -> (JSONObject) row)
                .sorted(Comparator.comparingInt(e -> e.getIntValue("index")))
                .toList();
        for (JSONObject nodeDetail : sortedDetails) {
            String nodeId = nodeDetail.getString("node_id");
            List<String> lastNodeIdList = nodeDetail.getJSONArray("upNodeIdList").toJavaList(String.class);
            if (nodeDetail.getString("runtimeNodeId").equals(startNodeId)) {
                nodeDetail.put("form_data", startNodeData);
                // 处理起始节点
                this.startNode = getNodeClsById(
                        nodeId,
                        lastNodeIdList,
                        n -> {
                            JSONObject nodeParams = n.getProperties();
                            boolean isResult = APPLICATION.name().equals(n.getType());
                            nodeParams.put("form_data", startNodeData);
                            //  nodeData.put("child_node", childNode);
                            nodeParams.put("isResult", isResult);
                            return nodeParams;
                        }
                );
                // 合并验证参数
                assert startNode != null;
                if (APPLICATION.getKey().equals(startNode.getType())) {
                    startNode.getContext().put("application_node_dict", nodeDetail.get("application_node_dict"));
                }
                startNode.saveContext(nodeDetail);
                nodeContext.add(startNode);
            } else {
                // 处理普通节点
                INode node = getNodeClsById(nodeId, lastNodeIdList, null);
                assert node != null;
                node.saveContext(nodeDetail);
                nodeContext.add(node);
            }
        }
    }


    public String run() {
        runChainManage(startNode, null);
        ChatMessageVO vo = new ChatMessageVO(chatParams.getChatId(), chatParams.getChatRecordId(), true);
        chatParams.getSink().tryEmitNext(vo);
        return answer;
    }


    public INode getStartNode() {
        return getNodeClsById(START.getKey(), List.of(), null);
    }

    public void runChainManage(INode currentNode, NodeResultFuture nodeResultFuture) {
        if (currentNode == null) {
            currentNode = getStartNode();
        }
        NodeResult result = runChainNode(currentNode, nodeResultFuture);
        // 获取下一个节点列表
        List<INode> nodeList = getNextNodeList(currentNode, result);
        if (nodeList.size() == 1) {
            runChainManage(nodeList.get(0), null);
        } else if (nodeList.size() > 1) {
            // 提交子任务并获取Future对象
            for (INode node : nodeList) {
                runChainManage(node, null);
            }
        }
    }


    public List<INode> getNextNodeList(INode currentNode, NodeResult currentNodeResult) {
        List<INode> nodeList = new ArrayList<>();
        if (currentNodeResult == null) {
            return nodeList;
        }
        // 判断是否中断执行
        if (currentNodeResult.isInterruptExec(currentNode)) {
            return nodeList;
        }
        if (currentNodeResult.isAssertionResult()) {
            // 处理断言结果分支
            for (LfEdge edge : edges) {
                if (edge.getSourceNodeId().equals(currentNode.getId())) {
                    // 构造预期的sourceAnchorId
                    Map<String, Object> nodeVariables = currentNodeResult.getNodeVariable();
                    String branchId = nodeVariables != null ? (String) nodeVariables.getOrDefault("branchId", "") : "";
                    String expectedAnchorId = String.format("%s_%s_right", currentNode.getId(), branchId);
                    if (expectedAnchorId.equals(edge.getSourceAnchorId())) {
                        processEdge(edge, currentNode, nodeList);
                    }
                }
            }
        } else {
            // 处理非断言结果分支
            for (LfEdge edge : edges) {
                if (edge.getSourceNodeId().equals(currentNode.getId())) {
                    processEdge(edge, currentNode, nodeList);
                }
            }
        }
        return nodeList;
    }


    private void processEdge(LfEdge edge, INode currentNode, List<INode> nodeList) {
        // 查找目标节点
        Optional<INode> targetNodeOpt = nodes.stream()
                .filter(node -> node.getId().equals(edge.getTargetNodeId()))
                .findFirst();
        if (targetNodeOpt.isEmpty()) {
            return;
        }
        INode targetNode = targetNodeOpt.get();
        String condition = (String) targetNode.getProperties().getOrDefault("condition", "AND");
        // 处理节点依赖
        if ("AND".equals(condition)) {
            if (dependentNodeBeenExecuted(edge.getTargetNodeId())) {
                addNodeToList(edge.getTargetNodeId(), currentNode, nodeList);
            }
        } else {
            addNodeToList(edge.getTargetNodeId(), currentNode, nodeList);
        }
    }

    private void addNodeToList(String targetNodeId, INode currentNode, List<INode> nodeList) {
        // 构建上游节点ID列表
        List<String> newUpNodeIds = new ArrayList<>();
        if (currentNode.getUpNodeIdList() != null) {
            newUpNodeIds.addAll(currentNode.getUpNodeIdList());
        }
        newUpNodeIds.add(currentNode.getId());
        // 获取节点实例并添加到列表
        INode nextNode = getNodeClsById(targetNodeId, newUpNodeIds, null);
        if (nextNode != null) {
            nodeList.add(nextNode);
        }
    }


    private INode getNodeClsById(String nodeId, List<String> upNodeIds, Function<INode, JSONObject> getNodeParams) {
        for (INode node : nodes) {
            if (nodeId.equals(node.getId())) {
                node.setUpNodes(nodeContext);
                node.setGlobalVariable(globalVariable);
                node.setUpNodeIdList(upNodeIds);
                node.setChatParams(chatParams);
                node.setHistoryChatRecords(historyChatRecords);
                if (getNodeParams != null) {
                    JSONObject properties = node.getProperties();
                    if (properties.containsKey("nodeData")) {
                        JSONObject nodeParams = getNodeParams.apply(node);
                        properties.put("nodeData", nodeParams);
                    }
                    node.setProperties(properties);
                }
                return node;
            }
        }
        return null;
    }


    public NodeResult runChainNode(INode currentNode, NodeResultFuture nodeResultFuture) {
        assert currentNode != null;
        // 添加节点
        appendNode(currentNode);
        // 处理默认的nodeResultFuture
        if (nodeResultFuture == null) {
            nodeResultFuture = runNodeFuture(currentNode);
        }
        NodeResult currentResult = nodeResultFuture.getResult();
        if (currentResult != null) {
            currentResult.writeContext(currentNode, this);
        }
        return currentResult;
    }


    public JSONObject getRuntimeDetails() {
        JSONObject detailsResult = new JSONObject();
        if (nodeContext == null || nodeContext.isEmpty()) {
            return detailsResult;
        }
        for (int index = 0; index < nodeContext.size(); index++) {
            INode node = nodeContext.get(index);
            JSONObject details = node.getDetail(index);
            details.put("node_id", node.getId());
            details.put("upNodeIdList", node.getUpNodeIdList());
            details.put("runtimeNodeId", node.getRuntimeNodeId());
            detailsResult.put(node.getRuntimeNodeId(), details);
        }
        return detailsResult;
    }

    public void appendNode(INode currentNode) {
        for (int i = 0; i < this.nodeContext.size(); i++) {
            INode node = this.nodeContext.get(i);
            if (currentNode.getId().equals(node.getId()) && currentNode.getRuntimeNodeId().equals(node.getRuntimeNodeId())) {
                this.nodeContext.set(i, currentNode);
                return;
            }
        }
        this.nodeContext.add(currentNode);
    }

    public NodeResultFuture runNodeFuture(INode node) {
        try {
            NodeResult result = node.run();
            return new NodeResultFuture(result, null, 200);
        } catch (Exception ex) {
            log.error("{} ERROR :{}", node.getType(), ex.getMessage());
            return new NodeResultFuture(null, ex, 500);
        }
    }

    private boolean hasNextNode(INode currentNode, NodeResult nodeResult) {
        if (nodeResult != null && nodeResult.isAssertionResult()) {
            for (LfEdge edge : edges) {
                if (edge.getSourceNodeId().equals(currentNode.getId())) {
                    String branchId = (String) nodeResult.getNodeVariable().get("branchId");
                    String expectedSourceAnchorId = String.format("%s_%s_right", edge.getSourceNodeId(), branchId);
                    if (expectedSourceAnchorId.equals(edge.getSourceAnchorId())) {
                        return true;
                    }
                }
            }
        } else {
            for (LfEdge edge : edges) {
                if (edge.getSourceNodeId().equals(currentNode.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isResult(INode currentNode, NodeResult currentNodeResult) {
        if (currentNode.getNodeData() == null) {
            return false;
        }
        boolean defaultVal = !hasNextNode(currentNode, currentNodeResult);
        Boolean isResult = currentNode.getNodeData().getBoolean("isResult");
        return isResult == null ? defaultVal : isResult;
    }

    public boolean dependentNode(String lastNodeId, INode node) {
        if (Objects.equals(lastNodeId, node.getId())) {
            if (FORM.getKey().equals(node.getId())) {
                Object formData = node.getContext().get("form_data");
                return formData != null;
            }
            return true;
        }
        return false;
    }

    public boolean dependentNodeBeenExecuted(String nodeId) {
        // 获取所有目标节点ID等于给定nodeId的边的源节点ID列表
        List<String> upNodeIdList = new ArrayList<>();
        for (LfEdge edge : edges) {
            if (edge.getTargetNodeId().equals(nodeId)) {
                upNodeIdList.add(edge.getSourceNodeId());
            }
        }
        // 检查每个上游节点是否都已执行
        return upNodeIdList.stream().allMatch(upNodeId ->
                this.nodeContext.stream().anyMatch(node -> dependentNode(upNodeId, node))
        );
    }


}
