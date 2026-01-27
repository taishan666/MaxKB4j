package com.tarzan.maxkb4j.core.workflow.model;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tarzan.maxkb4j.core.workflow.enums.DialogueType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeStatus;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.enums.WorkflowMode;
import com.tarzan.maxkb4j.core.workflow.logic.LfEdge;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domain.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.chat.dto.ChatParams;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Sinks;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Slf4j
@Data
public class Workflow {
    private AbsNode currentNode;
    private ChatParams chatParams;
    private List<AbsNode> nodes;
    private List<LfEdge> edges;
    private WorkflowMode workflowMode;
    private Map<String, Object> context;
    private Map<String, Object> chatContext;
    private List<AbsNode> nodeContext;
    private String answer;
    private List<ApplicationChatRecordEntity> historyChatRecords;
    @JsonIgnore
    private Sinks.Many<ChatMessageVO> sink;


    public Workflow(List<AbsNode> nodes, List<LfEdge> edges, ChatParams chatParams, Sinks.Many<ChatMessageVO> sink) {
        this.workflowMode=WorkflowMode.APPLICATION;
        this.nodes = nodes;
        this.edges = edges;
        this.chatParams = chatParams;
        this.context = new HashMap<>();
        this.chatContext = new HashMap<>();
        this.nodeContext = new CopyOnWriteArrayList<>();
        this.answer = "";
        this.historyChatRecords = CollectionUtils.isEmpty(chatParams.getHistoryChatRecords()) ? List.of() : chatParams.getHistoryChatRecords();
        if (StringUtils.isNotBlank(chatParams.getRuntimeNodeId()) && Objects.nonNull(chatParams.getChatRecord())) {
            this.loadNode(chatParams.getChatRecord(), chatParams.getRuntimeNodeId(), chatParams.getNodeData());
        }
        this.sink = sink;
    }

    public Workflow(WorkflowMode workflowMode, List<AbsNode> nodes, List<LfEdge> edges) {
        this.workflowMode=workflowMode;
        this.nodes = nodes;
        this.edges = edges;
        this.context = new HashMap<>();
        this.chatContext = new HashMap<>();
        this.nodeContext = new CopyOnWriteArrayList<>();
        this.answer = "";
        this.historyChatRecords = List.of();
        this.sink = null;
    }


    @SuppressWarnings("unchecked")
    private void loadNode(ApplicationChatRecordEntity chatRecord, String currentNodeId, Map<String, Object> currentNodeData) {
        List<Map<String, Object>> sortedDetails = chatRecord.getDetails().values().stream().map(row -> (Map<String, Object>) row).sorted(Comparator.comparingInt(e -> (int) e.get("index"))).toList();
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
                assert currentNode != null;
                currentNode.setStatus(nodeStatus);
                currentNode.saveContext(this, nodeDetail);
                currentNode.setDetail(nodeDetail);
                nodeContext.add(currentNode);
            } else {
                // 处理其他节点
                AbsNode node = getNodeClsById(nodeId, upNodeIdList, null);
                assert node != null;
                node.setStatus(nodeStatus);
                node.saveContext(this, nodeDetail);
                node.setDetail(nodeDetail);
                nodeContext.add(node);
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
                        nextNode.setStatus(NodeStatus.SKIP.getCode());
                    }
                }
            }
            // 获取节点实例并添加到列表
            return nextNode;
        }).collect(Collectors.toList());
    }


    public boolean dependentNodeBeenExecuted(AbsNode node) {
        List<String> upNodeIdList = edges.stream().filter(edge -> edge.getTargetNodeId().equals(node.getId())).map(LfEdge::getSourceNodeId).toList();// 构建上游节点ID列表
        //针对开始节点放行
        if (CollectionUtils.isEmpty(upNodeIdList)) {
            return true;
        }
        List<AbsNode> upNodes = nodes.stream().filter(e -> upNodeIdList.contains(e.getId())).toList();
        return upNodes.stream().allMatch(e -> (NodeStatus.SUCCESS.getCode()==e.getStatus() || NodeStatus.SKIP.getCode()==e.getStatus()));
    }

    // 是否是汇聚节点（排除上游节点都是SKIP的汇聚节点）
    public boolean isReadyJoinNode(AbsNode node) {
        List<String> upNodeIdList = edges.stream().filter(edge -> edge.getTargetNodeId().equals(node.getId())).map(LfEdge::getSourceNodeId).toList();// 构建上游节点ID列表
        if (CollectionUtils.isEmpty(upNodeIdList)) {
            return false;
        }
        if (upNodeIdList.size() > 1) {
            List<AbsNode> upNodes = nodes.stream().filter(e -> upNodeIdList.contains(e.getId())).toList();
            return !upNodes.stream().allMatch(e -> NodeStatus.SKIP.getCode()==e.getStatus());
        }
        return false;
    }


    public AbsNode getNodeClsById(String nodeId, List<String> upNodeIds, Function<AbsNode, JSONObject> getNodeProperties) {
        Optional<AbsNode> nodeOpt = nodes.stream().filter(Objects::nonNull).filter(e -> nodeId.equals(e.getId())).findFirst();
        if (nodeOpt.isPresent()) {
            AbsNode node = nodeOpt.get();
            node.setUpNodeIdList(upNodeIds);
            if (getNodeProperties != null) {
                getNodeProperties.apply(node);
            }
            return node;
        }
        return null;
    }

    private Map<String, Object> getPromptVariables() {
        Map<String, Object> result = new HashMap<>(100);
        for (String key : this.context.keySet()) {
            Object value = this.context.get(key);
            result.put("global." + key, value == null ? "*" : value);
        }
        for (String key : this.chatContext.keySet()) {
            Object value = this.chatContext.get(key);
            result.put("chat." + key, value == null ? "*" : value);
        }
        for (AbsNode node : nodeContext) {
            String nodeName = node.getProperties().getString("nodeName");
            Map<String, Object> context = node.getContext();
            for (String key : context.keySet()) {
                Object value = context.get(key);
                result.put(nodeName + "." + key, value == null ? "*" : value);
            }
        }
        if (this instanceof LoopWorkFlow loopWorkFlow){
            for (String key : loopWorkFlow.getLoopContext().keySet()) {
                Object value = loopWorkFlow.getLoopContext().get(key);
                result.put("loop." + key, value == null ? "*" : value);
            }
        }
        return result;
    }

    private Map<String, Map<String, Object>> getFlowVariables() {
        Map<String, Map<String, Object>> result = new HashMap<>(100);
        result.put("global", context);
        result.put("chat", chatContext);
        if (this instanceof LoopWorkFlow loopWorkFlow){
            result.put("loop", loopWorkFlow.getLoopContext());
        }
        for (AbsNode node : nodeContext) {
            result.put(node.getId(), node.getContext());
        }
        return result;
    }


    public JSONObject getRuntimeDetails() {
        JSONObject result = new JSONObject(true);
        if (nodeContext == null || nodeContext.isEmpty()) {
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

    public void appendNode(AbsNode currentNode) {
        for (int i = 0; i < this.nodeContext.size(); i++) {
            AbsNode node = this.nodeContext.get(i);
            if (currentNode.getId().equals(node.getId()) && currentNode.getRuntimeNodeId().equals(node.getRuntimeNodeId())) {
                this.nodeContext.set(i, currentNode);
                return;
            }
        }
        this.nodeContext.add(currentNode);
    }

    @SuppressWarnings("unchecked")
    public Object getFieldValue(Object value, String source) {
        if ("reference".equals(source)) {
            if (value instanceof List) {
                List<String> fields = (List<String>) value;
                return getReferenceField(fields.get(0), fields.get(1));
            }
        }
        return value;
    }

    public Object getReferenceField(String nodeId, String key) {
        Map<String, Object> nodeVariable = getFlowVariables().get(nodeId);
        return nodeVariable == null ? null : nodeVariable.get(key);
    }

    public AbsNode getNode(String nodeId) {
        return nodes.stream().filter(e -> nodeId.equals(e.getId())).findAny().orElse(null);
    }

    public String generatePrompt(String prompt) {
        return generatePrompt(prompt, Map.of());
    }

    public String generatePrompt(String prompt, Map<String, Object> addVariables) {
        if (StringUtils.isBlank(prompt)) {
            return "";
        }
        Map<String, Object> variables = new HashMap<>(getPromptVariables());
        variables.putAll(addVariables);
        PromptTemplate promptTemplate = PromptTemplate.from(prompt);
        return promptTemplate.apply(variables).text();
    }


    public List<ChatMessage> getHistoryMessages(int dialogueNumber, String dialogueType, String runtimeNodeId) {
        List<ChatMessage> historyMessages;
        if (DialogueType.NODE.name().equals(dialogueType)) {
            historyMessages = getNodeMessages(runtimeNodeId);
        } else {
            historyMessages = getWorkFlowMessages();
        }
        int total = historyMessages.size();
        if (total == 0) {
            return historyMessages;
        }
        int startIndex = Math.max(total - dialogueNumber * 2, 0);
        return historyMessages.subList(startIndex, total);
    }

    private List<ChatMessage> getWorkFlowMessages() {
        String regex = "<form_render>(.*?)</form_render>";
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        List<ChatMessage> messages = new ArrayList<>();
        for (ApplicationChatRecordEntity message : historyChatRecords) {
            String answerText = message.getAnswerText();
            Matcher matcher = pattern.matcher(answerText);
            if (!matcher.find()) {
                messages.add(new UserMessage(message.getProblemText()));
                messages.add(new AiMessage(message.getAnswerText()));
            }
        }
        return messages;
    }


    private List<ChatMessage> getNodeMessages(String runtimeNodeId) {
        List<ChatMessage> messages = new ArrayList<>();
        for (ApplicationChatRecordEntity record : historyChatRecords) {
            // 获取节点详情
            JSONObject nodeDetails = record.getNodeDetailsByRuntimeNodeId(runtimeNodeId);
            // 如果节点详情为空，返回空列表
            if (nodeDetails != null) {
                messages.add(new UserMessage(nodeDetails.getString("question")));
                messages.add(new AiMessage(nodeDetails.getString("answer")));
            }
        }
        return messages;
    }


}
