package com.tarzan.maxkb4j.module.application.wrokflow;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.wrokflow.dto.*;
import com.tarzan.maxkb4j.module.application.wrokflow.handler.WorkFlowPostHandler;
import lombok.Data;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

@Data
public class WorkflowManage {
    private String startNodeId;
    private INode startNode;
    private Map<String, Object> formData = new HashMap<>();
    private List<Object> imageList = new ArrayList<>();
    private List<Object> documentList = new ArrayList<>();
    private List<Object> audioList = new ArrayList<>();
    private FlowParams params;
    private Flow flow;
    private final ReentrantLock lock = new ReentrantLock();
    private Map<String, Object> context = new HashMap<>();
    private NodeChunkManage nodeChunkManage;
    private WorkFlowPostHandler workFlowPostHandler;
    private Object currentNode;
    private NodeResult currentResult;
    private String answer = "";
    private List<String> answerList = new ArrayList<>(Collections.singletonList(""));
    private int status = 200;
    private BaseToResponse baseToResponse;
    private ApplicationChatRecordEntity chatRecord;
    private Map<String, CompletableFuture<?>> awaitFutureMap = new HashMap<>();
    private Object childNode; // 根据实际需要定义类型
    private List<INode> nodeContext = new ArrayList<>();

    public WorkflowManage(Flow flow, FlowParams params, WorkFlowPostHandler workFlowPostHandler,
                          BaseToResponse baseToResponse, Map<String, Object> formData, List<Object> imageList,
                          List<Object> documentList, List<Object> audioList, String startNodeId,
                          Object startNodeData, ApplicationChatRecordEntity chatRecord, Object childNode) {
        if (formData != null) {
            this.formData = formData;
        }
        if (imageList != null) {
            this.imageList = imageList;
        }
        if (documentList != null) {
            this.documentList = documentList;
        }
        if (audioList != null) {
            this.audioList = audioList;
        }
        this.startNodeId = startNodeId;
        this.params = params;
        this.flow = flow;
        this.workFlowPostHandler = workFlowPostHandler;
        this.baseToResponse = baseToResponse;
        this.chatRecord = chatRecord;
        this.childNode = childNode;
        this.nodeChunkManage = new NodeChunkManage(this);

        if (startNodeId != null) {
            loadNode(chatRecord, startNodeId, startNodeData);
        }
    }

    public boolean answerIsNotEmpty() {
        if (answerList.isEmpty()) {
            return false;
        }
        String lastAnswer = answerList.get(answerList.size() - 1);
        return lastAnswer != null && !lastAnswer.trim().isEmpty();
    }


    public void appendAnswer(String content) {
        this.answer += content;
        this.answerList.add(content);
    }

    public List<Answer> getAnswerTextList() {
        List<Answer> answerList = nodeContext.stream()
                .map(INode::getAnswerList)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .toList();

        List<Answer> result = new ArrayList<>();
        Answer upNode = null;

        for (Answer currentAnswer : answerList) {
            if (!currentAnswer.getContent().isEmpty()) {
                if (upNode == null || "single_view".equals(currentAnswer.getViewType()) ||
                        ("many_view".equals(currentAnswer.getViewType()) && "single_view".equals(upNode.getViewType()))) {
                    result.add(currentAnswer);
                } else {
                    if (!result.isEmpty()) {
                        int execIndex = result.size() - 1;
                        String content = result.get(execIndex).getContent();
                        result.get(execIndex).setContent(content.isEmpty() ? currentAnswer.getContent() : content + "\n\n" + currentAnswer.getContent());
                    } else {
                        result.add(0, currentAnswer);
                    }
                }
                upNode = currentAnswer;
            }
        }

        if (result.isEmpty()) {
            // 如果没有响应 就响应一个空数据
            return Collections.singletonList(new Answer("", "", "", "", Collections.emptyMap()));
        }

        return result;
    }

    private void loadNode(Object chatRecord, String startNodeId, Object startNodeData) {
        // 实现细节取决于你的具体需求
    }

    public String generatePrompt(String prompt) {
        return "";
    }

    public Map<String, Map<String, Object>> getRuntimeDetails() {
        Map<String, Map<String, Object>> detailsResult = new HashMap<>();

        if (nodeContext == null || nodeContext.isEmpty()) {
            return detailsResult;
        }

        for (int index = 0; index < nodeContext.size(); index++) {
            INode node = nodeContext.get(index);

            JSONObject details = new JSONObject();

            if (chatRecord != null && chatRecord.getDetails() != null) {
                details = chatRecord.getDetails().getJSONObject(node.getRuntimeNodeId());
                if (details != null && !startNode.getRuntimeNodeId().equals(node.getRuntimeNodeId())) {
                    detailsResult.put(node.getRuntimeNodeId(), details);
                    continue;
                }
            }

            details = node.getDetails(index);
            details.put("node_id", node.getId());
            details.put("up_node_id_list", node.getUpNodeIdList());
            details.put("runtime_node_id", node.getRuntimeNodeId());

            detailsResult.put(node.getRuntimeNodeId(), details);
        }

        return detailsResult;
    }

    public void appendNode(INode currentNode) {
        for (int i = 0; i < this.nodeContext.size(); i++) {
            INode node = this.nodeContext.get(i);
            if (currentNode.id.equals(node.id) && currentNode.runtimeNodeId.equals(node.runtimeNodeId)) {
                this.nodeContext.set(i, node);
                return;
            }
        }
        this.nodeContext.add(currentNode);
    }

    public NodeResultFuture runNodeFuture(INode node) {
        try {
            node.validArgs(node.nodeParams, node.workflowParams);
            NodeResult result = node.run();
            return new NodeResultFuture(result, null, 200);
        } catch (Exception ex) {
            return new NodeResultFuture(null, ex, 500);
        }
    }

    private boolean hasNextNode(INode currentNode, NodeResult nodeResult) {
        if (nodeResult != null && nodeResult.isAssertionResult()) {
            for (Edge edge : flow.getEdges()) {
                if (edge.getSourceNodeId().equals(currentNode.id)) {
                    String branchId = (String) nodeResult.getNodeVariable().get("branch_id");
                    String expectedSourceAnchorId = String.format("%s_%s_right", edge.getSourceNodeId(), branchId);
                    if (expectedSourceAnchorId.equals(edge.getSourceNodeId())) {
                        return true;
                    }
                }
            }
        } else {
            for (Edge edge : flow.getEdges()) {
                if (edge.getSourceNodeId().equals(currentNode.id)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isResult(INode currentNode, NodeResult currentNodeResult) {
        if (currentNode.getNodeParams() == null) {
            return false;
        }
        boolean defaultVal = !hasNextNode(currentNode, currentNodeResult);
        Boolean isResult = currentNode.getNodeParams().getBoolean("is_result");
        return isResult==null?defaultVal:isResult;
    }

    public NodeResult runNode(INode node) {
        return node.run();
    }

    public boolean dependentNode(String lastNodeId, INode node){
        if(Objects.equals(lastNodeId, node.id)){
            if ("form-node".equals(node.type)){
               Object formData =node.getContext().get("form_data");
                return formData!=null;
            }
            return true;
        }
        return false;
    }

    public boolean dependentNodeBeenExecuted(String nodeId) {
        // 获取所有目标节点ID等于给定nodeId的边的源节点ID列表
        List<String> upNodeIdList = new ArrayList<>();
        for (Edge edge : this.flow.getEdges()) {
            if (edge.getTargetNodeId().equals(nodeId)) {
                upNodeIdList.add(edge.getSourceNodeId());
            }
        }

        // 检查每个上游节点是否都已执行
        for (String upNodeId : upNodeIdList) {
            boolean anyMatch = false;
            for (INode node : this.nodeContext) {
                if (dependentNode(upNodeId, node)) {
                    anyMatch = true;
                    break;
                }
            }
            if (!anyMatch) {
                return false;
            }
        }
        return true;
    }


}
