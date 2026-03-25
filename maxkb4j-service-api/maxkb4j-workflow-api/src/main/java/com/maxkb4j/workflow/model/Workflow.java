package com.maxkb4j.workflow.model;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.maxkb4j.common.domain.dto.*;
import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.enums.WorkflowMode;
import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.node.AbsNode;
import dev.langchain4j.data.message.ChatMessage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Sinks;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Workflow model class
 * Responsible for workflow configuration, node management and execution control
 * After refactoring, responsibilities are clearer:
 * - Use WorkflowContext for context management
 * - Use VariableResolver for variable resolution
 * - Use HistoryManager for history messages
 * - Use TemplateRenderer for template rendering
 */
@Slf4j
@Data
public class Workflow {

    private WorkflowMode workflowMode;
    private AbsNode currentNode;
    private ChatParams chatParams;
    private List<AbsNode> nodes;
    private List<LfEdge> edges;
    /**
     * Context manager
     * Get workflow context
     */
    private WorkflowContext workflowContext;
    /**
     * History message manager
     */
    private HistoryManager historyManager;
    /**
     * Variable resolver
     * Get variable resolver
     */
    private VariableResolver variableResolver;
    /**
     * Template renderer
     */
    private TemplateRenderer templateRenderer;

    /**
     * Cached node ID to node map for O(1) lookups
     */
    private Map<String, AbsNode> nodeMap;

    @JsonIgnore
    private Sinks.Many<ChatMessageVO> sink;


    public Workflow(WorkflowMode workflowMode, List<AbsNode> nodes, List<LfEdge> edges) {
        init(workflowMode, nodes, edges, ChatParams.builder().build(), Sinks.many().unicast().onBackpressureBuffer(), null);
    }

    public Workflow(WorkflowMode workflowMode, List<AbsNode> nodes, List<LfEdge> edges, ChatParams chatParams, Sinks.Many<ChatMessageVO> sink) {
        JSONObject details = null;
        if (chatParams != null && chatParams.getChatRecord() != null) {
            details = chatParams.getChatRecord().getDetails();
        }
        init(workflowMode, nodes, edges, chatParams, sink, details);
    }

    public Workflow(WorkflowMode workflowMode, List<AbsNode> nodes, List<LfEdge> edges, ChatParams chatParams, JSONObject details, Sinks.Many<ChatMessageVO> sink) {
        init(workflowMode, nodes, edges, chatParams, sink, details);
    }

    /**
     * Initialize workflow
     */
    private void init(WorkflowMode workflowMode, List<AbsNode> nodes, List<LfEdge> edges, ChatParams chatParams, Sinks.Many<ChatMessageVO> sink, JSONObject details) {
        this.workflowMode = workflowMode;
        this.nodes = nodes;
        this.edges = edges;
        this.chatParams = chatParams;
        this.sink = sink;
        this.workflowContext = new WorkflowContext();
        // Build node map for O(1) lookups
        this.nodeMap = nodes != null
                ? nodes.stream().filter(Objects::nonNull).collect(Collectors.toMap(AbsNode::getId, n -> n, (a, b) -> a))
                : new HashMap<>();
        // If chatParams is null, pass empty history list
        this.historyManager = new HistoryManager(chatParams != null ? chatParams.getHistoryChatRecords() : Collections.emptyList());
        this.variableResolver = new VariableResolver(this.workflowContext);
        this.templateRenderer = new TemplateRenderer(this.variableResolver);
        // Load node state
        if (chatParams != null && StringUtils.isNotBlank(chatParams.getRuntimeNodeId()) && Objects.nonNull(chatParams.getChatRecord())) {
            if (details != null) {
                this.loadNode(details, chatParams.getRuntimeNodeId(), chatParams.getNodeData());
            }
        }
    }


    @SuppressWarnings("unchecked")
    public void loadNode(JSONObject details, String currentNodeId, Map<String, Object> currentNodeData) {
        if (details == null || currentNodeId == null) {
            log.warn("loadNode called with null details or currentNodeId");
            return;
        }
        List<Map<String, Object>> sortedDetails = details.values().stream()
                .filter(Objects::nonNull)
                .map(row -> (Map<String, Object>) row)
                .sorted(Comparator.comparing(
                        e -> (Integer) e.get("index"),
                        Comparator.nullsLast(Comparator.naturalOrder())
                )).toList();
        for (Map<String, Object> nodeDetail : sortedDetails) {
            String nodeId = (String) nodeDetail.get("nodeId");
            List<String> upNodeIdList = (List<String>) nodeDetail.get("upNodeIdList");
            String runtimeNodeId = (String) nodeDetail.get("runtimeNodeId");
            Integer nodeStatus = (Integer) nodeDetail.get("status");
            if (runtimeNodeId.equals(currentNodeId)) {
                // Process start node
                this.currentNode = getNodeClsById(nodeId, upNodeIdList, n -> {
                    JSONObject nodeProperties = n.getProperties();
                    if (nodeProperties.containsKey("nodeData")) {
                        JSONObject nodeParams = nodeProperties.getJSONObject("nodeData");
                        nodeParams.put("form_data", currentNodeData);
                    }
                    return nodeProperties;
                });
                if (currentNode != null) {
                    currentNode.setStatus(nodeStatus);
                    currentNode.saveContext(this, nodeDetail);
                    currentNode.setDetail(nodeDetail);
                    workflowContext.appendNode(currentNode);
                }
            } else {
                // Process other nodes
                AbsNode node = getNodeClsById(nodeId, upNodeIdList, null);
                if (node != null) {
                    node.setStatus(nodeStatus);
                    node.saveContext(this, nodeDetail);
                    node.setDetail(nodeDetail);
                    workflowContext.appendNode(node);
                }
            }
        }
    }

    public AbsNode getStartNode() {
        return getNodeClsById(NodeType.START.getKey(), List.of(), null);
    }

    public List<AbsNode> getNextNodeList(AbsNode currentNode, NodeResult currentNodeResult) {
        // Check if execution should be interrupted
        if (currentNodeResult == null || currentNodeResult.isInterruptExec(currentNode)) {
            return List.of();
        }
        // Process non-assertion result branches
        List<LfEdge> sourceEdges = findDownstreamEdges(currentNode.getId());
        if (sourceEdges.isEmpty()) {
            return List.of();
        }
        List<String> targetNodeIds = sourceEdges.stream()
                .map(LfEdge::getTargetNodeId)
                .distinct()
                .toList();
        if (currentNodeResult.isAssertionResult()) {
            List<AbsNode> targetNodes = buildNodes(targetNodeIds, currentNode);
            targetNodes.forEach(e -> {
                if (!isAssertionNode(e.getId(), currentNodeResult, sourceEdges)) {
                    e.setStatus(NodeStatus.SKIP.getStatus());
                }
            });
            return targetNodes;
        } else {
            return buildNodes(targetNodeIds, currentNode);
        }
    }

    private boolean isAssertionNode(String nodeId, NodeResult currentNodeResult, List<LfEdge> sourceEdges) {
        List<String> assertionNodeIds = sourceEdges.stream().filter(edge -> {
            Map<String, Object> nodeVariables = currentNodeResult.getNodeVariable();
            String branchId = nodeVariables != null ? (String) nodeVariables.getOrDefault("branchId", "") : "";
            String expectedAnchorId = String.format("%s_%s_right", edge.getSourceNodeId(), branchId);
            return expectedAnchorId.equals(edge.getSourceAnchorId());
        }).map(LfEdge::getTargetNodeId).toList();
        return CollectionUtils.isNotEmpty(assertionNodeIds) && assertionNodeIds.contains(nodeId);
    }

    private List<AbsNode> buildNodes(List<String> targetNodeIds, AbsNode currentNode) {
        List<String> upNodeIdList = new ArrayList<>(currentNode.getUpNodeIdList());
        upNodeIdList.add(currentNode.getId());
        return targetNodeIds.stream()
                .map(nodeId -> getNodeClsById(nodeId, upNodeIdList, null))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Find upstream node IDs for a given node.
     */
    private List<String> findUpstreamNodeIds(String nodeId) {
        return edges.stream()
                .filter(edge -> nodeId.equals(edge.getTargetNodeId()))
                .map(LfEdge::getSourceNodeId)
                .toList();
    }

    /**
     * Find downstream edges for a given node.
     */
    private List<LfEdge> findDownstreamEdges(String nodeId) {
        return edges.stream()
                .filter(edge -> nodeId.equals(edge.getSourceNodeId()))
                .toList();
    }

    /**
     * Get nodes that are in the current workflow configuration.
     */
    private List<AbsNode> getValidNodeContext() {
        Set<String> nodeIds = nodeMap.keySet();
        return workflowContext.getNodeContext().stream()
                .filter(e -> nodeIds.contains(e.getId()))
                .toList();
    }

    public boolean dependentNodeBeenExecuted(AbsNode node) {
        List<String> upNodeIdList = findUpstreamNodeIds(node.getId());
        // Allow start node to pass
        if (CollectionUtils.isEmpty(upNodeIdList)) {
            return true;
        }
        Set<String> upNodeIdSet = new HashSet<>(upNodeIdList);
        return nodes.stream()
                .filter(e -> upNodeIdSet.contains(e.getId()))
                .allMatch(e -> NodeStatus.SUCCESS.getStatus() == e.getStatus() || NodeStatus.SKIP.getStatus() == e.getStatus());
    }

    // Check if it's a join node (excluding join nodes where all upstream nodes are SKIP)
    public boolean isReadyJoinNode(AbsNode node) {
        List<String> upNodeIdList = findUpstreamNodeIds(node.getId());
        if (CollectionUtils.isEmpty(upNodeIdList)) {
            return false;
        }
        if (upNodeIdList.size() > 1) {
            Set<String> upNodeIdSet = new HashSet<>(upNodeIdList);
            return !nodes.stream()
                    .filter(e -> upNodeIdSet.contains(e.getId()))
                    .allMatch(e -> NodeStatus.SKIP.getStatus() == e.getStatus());
        }
        return false;
    }

    public AbsNode getNodeClsById(String nodeId, List<String> upNodeIds, Function<AbsNode, JSONObject> getNodeProperties) {
        AbsNode node = nodeMap.get(nodeId);
        if (node != null) {
            node.setUpNodeIdList(upNodeIds);
            node.setTemplateRenderer(templateRenderer);
            if (getNodeProperties != null) {
                getNodeProperties.apply(node);
            }
        }
        return node;
    }

    public List<Answer> getAnswerTextList() {
        List<AbsNode> nodeContext = getValidNodeContext();
        if (nodeContext.isEmpty()) {
            return List.of();
        }
        List<Answer> answerTextList = new ArrayList<>(nodeContext.size());
        for (AbsNode node : nodeContext) {
            answerTextList.addAll(node.getAnswerList(chatParams.getChatRecordId()));
        }
        return answerTextList;
    }

    public JSONObject getRuntimeDetails() {
        JSONObject result = new JSONObject(true);
        List<AbsNode> nodeContext = getValidNodeContext();
        if (nodeContext.isEmpty()) {
            return result;
        }
        for (int index = 0; index < nodeContext.size(); index++) {
            AbsNode node = nodeContext.get(index);
            JSONObject runtimeDetail = new JSONObject(true);
            runtimeDetail.putAll(node.getDetail());
            runtimeDetail.put("index", index);
            runtimeDetail.put("nodeId", node.getId());
            runtimeDetail.put("name", node.getProperties().getString("nodeName"));
            runtimeDetail.put("upNodeIdList", node.getUpNodeIdList());
            runtimeDetail.put("runtimeNodeId", node.getRuntimeNodeId());
            runtimeDetail.put("type", node.getType());
            runtimeDetail.put("status", node.getStatus());
            runtimeDetail.put("errMessage", node.getErrMessage());
            result.put(node.getRuntimeNodeId(), runtimeDetail);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public Object getFieldValue(Object value, String source) {
        if ("reference".equals(source)) {
            if (value instanceof List) {
                List<String> fields = (List<String>) value;
                return variableResolver.getReferenceField(fields.get(0), fields.get(1));
            }
        }
        return value;
    }

    public Object getReferenceField(List<String> reference) {
        if (CollectionUtils.isNotEmpty(reference) && reference.size() > 1) {
            return variableResolver.getReferenceField(reference.get(0), reference.get(1));
        }
        return null;
    }

    public AbsNode getNode(String nodeId) {
        return nodeMap.get(nodeId);
    }

    public String renderPrompt(String prompt) {
        return templateRenderer.render(prompt);
    }

    public List<ChatRecordDTO> getHistoryChatRecords() {
        return historyManager.historyChatRecords();
    }

    public List<ChatMessage> getHistoryMessages(int dialogueNumber, String dialogueType, String runtimeNodeId) {
        return historyManager.getHistoryMessages(dialogueNumber, dialogueType, runtimeNodeId);
    }

    public Map<String, Object> getChatContext() {
        return workflowContext.getChatContext();
    }

    public Map<String, Object> getContext() {
        return workflowContext.getGlobalContext();
    }



    /**
     * Determine if current workflow needs sink output
     * Knowledge workflow doesn't need output, chat workflow needs output
     *
     * @return whether sink output is needed
     */
    public boolean needsSinkOutput() {
        return workflowMode == WorkflowMode.APPLICATION || workflowMode == WorkflowMode.APPLICATION_LOOP;
    }


}
