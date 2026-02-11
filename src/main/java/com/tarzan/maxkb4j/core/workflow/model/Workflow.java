package com.tarzan.maxkb4j.core.workflow.model;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tarzan.maxkb4j.core.workflow.context.WorkflowContext;
import com.tarzan.maxkb4j.core.workflow.enums.NodeStatus;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.enums.WorkflowMode;
import com.tarzan.maxkb4j.core.workflow.logic.LfEdge;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.service.HistoryManager;
import com.tarzan.maxkb4j.core.workflow.service.TemplateRenderer;
import com.tarzan.maxkb4j.core.workflow.service.VariableResolver;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domain.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.chat.dto.ChatParams;
import dev.langchain4j.data.message.ChatMessage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Sinks;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 工作流模型类
 * 负责工作流的配置、节点管理和执行控制
 * 重构后职责更加清晰：
 * - 使用 WorkflowContext 管理上下文
 * - 使用 VariableResolver 处理变量解析
 * - 使用 HistoryManager 管理历史消息
 * - 使用 TemplateRenderer 渲染模板
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
     * 上下文管理器
     *  获取工作流上下文
     */
    private WorkflowContext workflowContext;
    /**
     * 历史消息管理器
     */
    private HistoryManager historyManager;
    /**
     * 变量解析器
     *  获取变量解析器
     */
    private VariableResolver variableResolver;
    /**
     * 模板渲染器
     */
    private TemplateRenderer templateRenderer;

    @JsonIgnore
    private Sinks.Many<ChatMessageVO> sink;


    public Workflow(WorkflowMode workflowMode, List<AbsNode> nodes, List<LfEdge> edges) {
        init(workflowMode, nodes, edges, ChatParams.builder().build(), Sinks.many().unicast().onBackpressureBuffer(), null);
    }

    public Workflow(WorkflowMode workflowMode, List<AbsNode> nodes, List<LfEdge> edges, ChatParams chatParams,Sinks.Many<ChatMessageVO> sink) {
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
     * 初始化工作流
     */
    private void init(WorkflowMode workflowMode, List<AbsNode> nodes, List<LfEdge> edges, ChatParams chatParams, Sinks.Many<ChatMessageVO> sink, JSONObject details) {
        this.workflowMode = workflowMode;
        this.nodes = nodes;
        this.edges = edges;
        this.chatParams = chatParams;
        this.sink = sink;
        this.workflowContext = new WorkflowContext();
        // 如果chatParams为null，传入空的历史记录列表
        this.historyManager = new HistoryManager(chatParams != null ? chatParams.getHistoryChatRecords() : Collections.emptyList());
        this.variableResolver = new VariableResolver(this.workflowContext);
        this.templateRenderer = new TemplateRenderer(this.variableResolver);
        // 加载节点状态
        if (chatParams != null && StringUtils.isNotBlank(chatParams.getRuntimeNodeId()) && Objects.nonNull(chatParams.getChatRecord())) {
            if (details != null) {
                this.loadNode(details, chatParams.getRuntimeNodeId(), chatParams.getNodeData());
            }
        }
    }


    @SuppressWarnings("unchecked")
    public void loadNode(JSONObject details, String currentNodeId, Map<String, Object> currentNodeData) {
        List<Map<String, Object>> sortedDetails = details.values().stream()
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
                // 处理起始节点
                this.currentNode = getNodeClsById(nodeId, upNodeIdList, n -> {
                    JSONObject nodeProperties = n.getProperties();
                    if (nodeProperties.containsKey("nodeData")) {
                        JSONObject nodeParams = nodeProperties.getJSONObject("nodeData");
                        nodeParams.put("form_data", currentNodeData);
                    }
                    return nodeProperties;
                });
               if (currentNode!= null){
                   currentNode.setStatus(nodeStatus);
                   currentNode.saveContext(this, nodeDetail);
                   currentNode.setDetail(nodeDetail);
                   workflowContext.appendNode(currentNode);
               }
            } else {
                // 处理其他节点
                AbsNode node = getNodeClsById(nodeId, upNodeIdList, null);
                if (node != null){
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
        // 判断是否中断执行
        if (currentNodeResult == null || currentNodeResult.isInterruptExec(currentNode)) {
            return List.of();
        }
        // 处理非断言结果分支
        List<LfEdge> sourceEdges = edges.stream().filter(edge -> edge.getSourceNodeId().equals(currentNode.getId())).toList();
        // 获取节点实例并添加到列表
        return sourceEdges.stream().map(edge -> {
            List<String> upNodeIdList = new ArrayList<>(currentNode.getUpNodeIdList());
            upNodeIdList.add(currentNode.getId());
            AbsNode nextNode = getNodeClsById(edge.getTargetNodeId(), upNodeIdList, null);
            if (currentNodeResult.isAssertionResult()) {
                if (edge.getSourceNodeId().equals(currentNode.getId())) {
                    Map<String, Object> nodeVariables = currentNodeResult.getNodeVariable();
                    String branchId = nodeVariables != null ? (String) nodeVariables.getOrDefault("branchId", "") : "";
                    String expectedAnchorId = String.format("%s_%s_right", currentNode.getId(), branchId);
                    if (!expectedAnchorId.equals(edge.getSourceAnchorId())) {
                        assert nextNode != null;
                        nextNode.setStatus(NodeStatus.SKIP.getStatus());
                    }
                }
            }
            // 获取节点实例并添加到列表
            return nextNode;
        }).collect(Collectors.toList());
    }

    public boolean dependentNodeBeenExecuted(AbsNode node) {
        List<String> upNodeIdList = edges.stream().filter(edge -> edge.getTargetNodeId().equals(node.getId())).map(LfEdge::getSourceNodeId).toList();
        //针对开始节点放行
        if (CollectionUtils.isEmpty(upNodeIdList)) {
            return true;
        }
        List<AbsNode> upNodes = nodes.stream().filter(e -> upNodeIdList.contains(e.getId())).toList();
        return upNodes.stream().allMatch(e -> (NodeStatus.SUCCESS.getStatus()==e.getStatus() || NodeStatus.SKIP.getStatus()==e.getStatus()));
    }

    // 是否是汇聚节点（排除上游节点都是SKIP的汇聚节点）
    public boolean isReadyJoinNode(AbsNode node) {
        List<String> upNodeIdList = edges.stream().filter(edge -> edge.getTargetNodeId().equals(node.getId())).map(LfEdge::getSourceNodeId).toList();
        if (CollectionUtils.isEmpty(upNodeIdList)) {
            return false;
        }
        if (upNodeIdList.size() > 1) {
            List<AbsNode> upNodes = nodes.stream().filter(e -> upNodeIdList.contains(e.getId())).toList();
            return !upNodes.stream().allMatch(e -> NodeStatus.SKIP.getStatus()==e.getStatus());
        }
        return false;
    }

    public AbsNode getNodeClsById(String nodeId, List<String> upNodeIds, Function<AbsNode, JSONObject> getNodeProperties) {
        Optional<AbsNode> nodeOpt = nodes.stream().filter(Objects::nonNull).filter(e -> nodeId.equals(e.getId())).findFirst();
        if (nodeOpt.isPresent()) {
            AbsNode node = nodeOpt.get();
            node.setUpNodeIdList(upNodeIds);
            node.setTemplateRenderer(templateRenderer);
            if (getNodeProperties != null) {
                getNodeProperties.apply(node);
            }
            return node;
        }
        return null;
    }

    public List<Answer> getAnswerTextList() {
        List<AbsNode> nodeContext=workflowContext.getNodeContext().stream().filter(e ->nodes.stream().anyMatch(n -> e.getId().equals(n.getId()))).toList();
        if (nodeContext.isEmpty()) {
            return List.of();
        }
        List<Answer> answerTextList = new ArrayList<>();
        for (AbsNode node : nodeContext) {
            answerTextList.addAll(node.getAnswerList());
        }
        return answerTextList;
    }

    public JSONObject getRuntimeDetails() {
        JSONObject result = new JSONObject(true);
        List<AbsNode> nodeContext=workflowContext.getNodeContext().stream().filter(e ->nodes.stream().anyMatch(n -> e.getId().equals(n.getId()))).toList();
        if (nodeContext.isEmpty()) {
            return result;
        }
        for (int index = 0; index < nodeContext.size(); index++) {
            AbsNode node = nodeContext.get(index);
            JSONObject runtimeDetail = new JSONObject(true);
            runtimeDetail.put("index", index);
            runtimeDetail.put("nodeId", node.getId());
            runtimeDetail.put("name", node.getProperties().getString("nodeName"));
            runtimeDetail.put("upNodeIdList", node.getUpNodeIdList());
            runtimeDetail.put("runtimeNodeId", node.getRuntimeNodeId());
            runtimeDetail.put("type", node.getType());
            runtimeDetail.put("status", node.getStatus());
            runtimeDetail.put("errMessage", node.getErrMessage());
            runtimeDetail.putAll(node.getDetail());
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
        if (CollectionUtils.isNotEmpty(reference)&&reference.size()>1){
            return variableResolver.getReferenceField(reference.get(0), reference.get(1));
        }
        return null;
    }

    public AbsNode getNode(String nodeId) {
        return nodes.stream().filter(e -> nodeId.equals(e.getId())).findAny().orElse(null);
    }

    public String renderPrompt(String prompt) {
        return templateRenderer.render(prompt);
    }

    public List<ApplicationChatRecordEntity>  getHistoryChatRecords() {
        return historyManager.historyChatRecords();
    }

    public List<ChatMessage> getHistoryMessages(int dialogueNumber, String dialogueType, String runtimeNodeId) {
        return historyManager.getHistoryMessages(dialogueNumber, dialogueType, runtimeNodeId);
    }

    public Map<String,Object> getChatContext() {
        return workflowContext.getChatContext();
    }

    public Map<String,Object> getContext() {
        return workflowContext.getGlobalContext();
    }

    /**
     * 判断当前工作流是否需要输出到Sink
     * 知识库工作流不需要输出，对话工作流需要输出
     * @return 是否需要输出到Sink
     */
    public boolean needsSinkOutput() {
        return workflowMode == WorkflowMode.APPLICATION || workflowMode == WorkflowMode.APPLICATION_LOOP;
    }


}
