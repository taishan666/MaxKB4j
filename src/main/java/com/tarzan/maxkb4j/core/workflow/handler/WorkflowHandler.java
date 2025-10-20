package com.tarzan.maxkb4j.core.workflow.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WorkflowHandler {


   /* public String execute(Workflow workflow) {
        runChainManage(workflow, workflow.getStartNode());
        ChatMessageVO vo = new ChatMessageVO(workflow.getChatParams().getChatId(), workflow.getChatParams().getChatRecordId(), true);
        workflow.getChatParams().getSink().tryEmitNext(vo);
        return workflow.getAnswer();
    }

    public void runChainManage(Workflow workflow, INode currentNode) {
        if (currentNode == null) {
            currentNode = workflow.getStartNode();
        }
        NodeResult result = runChainNode(workflow, currentNode);
        // 获取下一个节点列表
        List<INode> nodeList = getNextNodeList(workflow, currentNode, result);
        if (nodeList.size() == 1) {
            runChainManage(workflow, nodeList.get(0));
        } else if (nodeList.size() > 1) {
            // 提交子任务并获取Future对象
            for (INode node : nodeList) {
                runChainManage(workflow, node);
            }
        }
    }

    public NodeResult runChainNode(Workflow workflow, INode currentNode) {
        assert currentNode != null;
        // 添加节点
        appendNode(workflow, currentNode);
        // 处理默认的nodeResultFuture
        NodeResultFuture nodeResultFuture = runNodeFuture(currentNode);
        NodeResult currentResult = nodeResultFuture.getResult();
        if (currentResult != null) {
            currentResult.writeContext(currentNode, workflow);
        }
        return currentResult;
    }

    public NodeResultFuture runNodeFuture(INode node) {
        try {
            NodeResult result = node.run();
            return new NodeResultFuture(result, null, 200);
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("NODE: {} ERROR :{}", node.getType(), ex.getCause().getMessage());
            return new NodeResultFuture(null, ex, 500);
        }
    }

    public void appendNode(Workflow workflow, INode currentNode) {
        for (int i = 0; i < workflow.getNodeContext().size(); i++) {
            INode node = workflow.getNodeContext().get(i);
            if (currentNode.getId().equals(node.getId()) && currentNode.getRuntimeNodeId().equals(node.getRuntimeNodeId())) {
                workflow.getNodeContext().set(i, currentNode);
                return;
            }
        }
        workflow.getNodeContext().add(currentNode);
    }

    public List<INode> getNextNodeList(Workflow workflow, INode currentNode, NodeResult currentNodeResult) {
        List<INode> nodeList = new ArrayList<>();
        // 判断是否中断执行
        if (currentNodeResult == null || currentNodeResult.isInterruptExec(currentNode)) {
            return nodeList;
        }
        if (currentNodeResult.isAssertionResult()) {
            // 处理断言结果分支
            for (LfEdge edge : workflow.getEdges()) {
                if (edge.getSourceNodeId().equals(currentNode.getId())) {
                    // 构造预期的sourceAnchorId
                    Map<String, Object> nodeVariables = currentNodeResult.getNodeVariable();
                    String branchId = nodeVariables != null ? (String) nodeVariables.getOrDefault("branchId", "") : "";
                    String expectedAnchorId = String.format("%s_%s_right", currentNode.getId(), branchId);
                    if (expectedAnchorId.equals(edge.getSourceAnchorId())) {
                        processEdge(workflow, edge, currentNode, nodeList);
                    }
                }
            }
        } else {
            // 处理非断言结果分支
            for (LfEdge edge : workflow.getEdges()) {
                if (edge.getSourceNodeId().equals(currentNode.getId())) {
                    processEdge(workflow, edge, currentNode, nodeList);
                }
            }
        }
        return nodeList;
    }

    private void processEdge(Workflow workflow, LfEdge edge, INode currentNode, List<INode> nodeList) {
        // 查找目标节点
        Optional<LfNode> targetNodeOpt = workflow.getLfNodes().stream()
                .filter(node -> node.getId().equals(edge.getTargetNodeId()))
                .findFirst();
        if (targetNodeOpt.isEmpty()) {
            return;
        }
        LfNode targetNode = targetNodeOpt.get();
        String condition = (String) targetNode.getProperties().getOrDefault("condition", "AND");
        // 处理节点依赖
        if ("AND".equals(condition)) {
            if (dependentNodeBeenExecuted(workflow, edge.getTargetNodeId())) {
                addNodeToList(workflow, edge.getTargetNodeId(), currentNode, nodeList);
            }
        } else {
            addNodeToList(workflow, edge.getTargetNodeId(), currentNode, nodeList);
        }
    }


    public boolean dependentNodeBeenExecuted(Workflow workflow, String nodeId) {
        // 获取所有目标节点ID等于给定nodeId的边的源节点ID列表
        List<String> upNodeIdList = new ArrayList<>();
        for (LfEdge edge : workflow.getEdges()) {
            if (edge.getTargetNodeId().equals(nodeId)) {
                upNodeIdList.add(edge.getSourceNodeId());
            }
        }
        // 检查每个上游节点是否都已执行
        return upNodeIdList.stream().allMatch(upNodeId ->
                workflow.getNodeContext().stream().anyMatch(node -> dependentNode(upNodeId, node))
        );
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

    private void addNodeToList(Workflow workflow, String targetNodeId, INode currentNode, List<INode> nodeList) {
        // 构建上游节点ID列表
        List<String> newUpNodeIds = new ArrayList<>();
        if (currentNode.getUpNodeIdList() != null) {
            newUpNodeIds.addAll(currentNode.getUpNodeIdList());
        }
        newUpNodeIds.add(currentNode.getId());
        // 获取节点实例并添加到列表
        INode nextNode = workflow.getNodeClsById(targetNodeId, newUpNodeIds, null);
        if (nextNode != null) {
            nodeList.add(nextNode);
        }
    }
*/

}
