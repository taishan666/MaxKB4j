package com.tarzan.maxkb4j.core.workflow.model;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tarzan.maxkb4j.core.workflow.enums.DialogueType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeStatus;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.logic.LfEdge;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.chat.dto.ChatParams;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Slf4j
@Data
public class Workflow {
    private INode currentNode;
    private ChatParams chatParams;
    private List<INode> nodes;
    private List<LfEdge> edges;
    private Map<String, Object> context;
    private Map<String, Object> chatContext;
    private List<INode> nodeContext;
    private String answer;
    private ApplicationChatRecordEntity chatRecord;
    private List<ApplicationChatRecordEntity> historyChatRecords;


    public Workflow(List<INode> nodes, List<LfEdge> edges, ChatParams chatParams, ApplicationChatRecordEntity chatRecord, List<ApplicationChatRecordEntity> historyChatRecords) {
        this.nodes = nodes;
        this.edges = edges;
        this.chatParams = chatParams;
        this.context = new HashMap<>();
        this.chatContext = new HashMap<>();
        this.nodeContext = new CopyOnWriteArrayList<>();
        this.chatRecord = chatRecord;
        this.answer = "";
        this.historyChatRecords = CollectionUtils.isEmpty(historyChatRecords) ? List.of() : historyChatRecords;
        if (StringUtils.isNotBlank(chatParams.getRuntimeNodeId()) && Objects.nonNull(chatRecord)) {
            this.loadNode(chatRecord, chatParams.getRuntimeNodeId(), chatParams.getNodeData());
        }
    }


    @SuppressWarnings("unchecked")
    private void loadNode(ApplicationChatRecordEntity chatRecord, String currentNodeId, Map<String, Object> currentNodeData) {
        List<Map<String, Object>> sortedDetails = chatRecord.getDetails().values().stream().map(row -> (Map<String, Object>) row).sorted(Comparator.comparingInt(e -> (int) e.get("index"))).toList();
        for (Map<String, Object> nodeDetail : sortedDetails) {
            String nodeId = (String) nodeDetail.get("nodeId");
            List<String> upNodeIdList = (List<String>) nodeDetail.get("upNodeIdList");
            String runtimeNodeId = (String) nodeDetail.get("runtimeNodeId");
            NodeStatus status = (NodeStatus) nodeDetail.get("status");
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
                currentNode.setStatus(status);
                currentNode.setDetail(nodeDetail);
                currentNode.saveContext(this, nodeDetail);
                nodeContext.add(currentNode);
            } else {
                // 处理其他节点
                INode node = getNodeClsById(nodeId, upNodeIdList, null);
                assert node != null;
                node.setStatus(status);
                node.setDetail(nodeDetail);
                node.saveContext(this, nodeDetail);
                nodeContext.add(node);
            }
        }
    }


    public INode getStartNode() {
        return getNodeClsById(NodeType.START.getKey(), List.of(), null);
    }


    public List<INode> getNextNodeList(INode currentNode, NodeResult currentNodeResult) {
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
            INode nextNode = getNodeClsById(edge.getTargetNodeId(), upNodeIdList, null);
            if (currentNodeResult.isAssertionResult()) {
                if (edge.getSourceNodeId().equals(currentNode.getId())) {
                    Map<String, Object> nodeVariables = currentNodeResult.getNodeVariable();
                    String branchId = nodeVariables != null ? (String) nodeVariables.getOrDefault("branchId", "") : "";
                    String expectedAnchorId = String.format("%s_%s_right", currentNode.getId(), branchId);
                    if (!expectedAnchorId.equals(edge.getSourceAnchorId())){
                        assert nextNode != null;
                        nextNode.setStatus(NodeStatus.SKIP);
                    }
                }
            }
            // 获取节点实例并添加到列表
            return nextNode;
        }).collect(Collectors.toList());
    }


    public boolean dependentNodeBeenExecuted(INode node) {
        List<String> upNodeIdList = edges.stream().filter(edge -> edge.getTargetNodeId().equals(node.getId())).map(LfEdge::getSourceNodeId).toList();// 构建上游节点ID列表
        if (CollectionUtils.isEmpty(upNodeIdList)){
            return true;
        }
        List<INode> upNodes=nodes.stream().filter(e -> upNodeIdList.contains(e.getId())).toList();
        boolean hasReadyNode = upNodes.stream().anyMatch(e -> e.getStatus().equals(NodeStatus.READY));
        if (hasReadyNode){
            return false;
        }
        return upNodes.stream().noneMatch(e -> NodeStatus.INTERRUPT.equals(e.getStatus()));
    }

    // 是否是汇聚节点
    public boolean isJoinNode(INode node) {
        List<String> upNodeIdList = edges.stream().filter(edge -> edge.getTargetNodeId().equals(node.getId())).map(LfEdge::getSourceNodeId).toList();// 构建上游节点ID列表
        if (CollectionUtils.isEmpty(upNodeIdList)){
            return false;
        }
        return upNodeIdList.size()>1;
    }


    private INode getNodeClsById(String nodeId, List<String> upNodeIds, Function<INode, JSONObject> getNodeProperties) {
        Optional<INode> nodeOpt =nodes.stream().filter(Objects::nonNull).filter(e -> nodeId.equals(e.getId())).findFirst();
        if (nodeOpt.isPresent()) {
            INode node = nodeOpt.get();
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
        for (INode node : nodeContext) {
            String nodeName = node.getProperties().getString("nodeName");
            Map<String, Object> context = node.getContext();
            for (String key : context.keySet()) {
                Object value = context.get(key);
                result.put(nodeName + "." + key, value == null ? "*" : value);
            }
        }
        return result;
    }

    private Map<String, Map<String, Object>> getFlowVariables() {
        Map<String, Map<String, Object>> result = new HashMap<>(100);
        result.put("global", context);
        result.put("chat", chatContext);
        for (INode node : nodeContext) {
            result.put(node.getId(), node.getContext());
        }
        return result;
    }


    public JSONObject getRuntimeDetails() {
        JSONObject detailsResult = new JSONObject();
        if (nodeContext == null || nodeContext.isEmpty()) {
            return detailsResult;
        }
        for (int index = 0; index < nodeContext.size(); index++) {
            INode node = nodeContext.get(index);
            JSONObject runtimeDetail = new JSONObject();
            runtimeDetail.putAll(node.executeDetail());
            runtimeDetail.put("nodeId", node.getId());
            runtimeDetail.put("upNodeIdList", node.getUpNodeIdList());
            runtimeDetail.put("runtimeNodeId", node.getRuntimeNodeId());
            runtimeDetail.put("name", node.getProperties().getString("nodeName"));
            runtimeDetail.put("index", index);
            runtimeDetail.put("type", node.getType());
            runtimeDetail.put("status", node.getStatus());
            runtimeDetail.put("errMessage", node.getErrMessage());
            detailsResult.put(node.getRuntimeNodeId(), runtimeDetail);
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
        Boolean isResult = currentNode.getNodeData().getBoolean("isResult");
        return isResult != null ? isResult : !hasNextNode(currentNode, currentNodeResult);
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

    public INode getNode(String nodeId) {
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
