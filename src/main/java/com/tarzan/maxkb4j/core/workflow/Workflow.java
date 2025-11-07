package com.tarzan.maxkb4j.core.workflow;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tarzan.maxkb4j.common.util.StringUtil;
import com.tarzan.maxkb4j.core.workflow.enums.DialogueType;
import com.tarzan.maxkb4j.core.workflow.factory.NodeFactory;
import com.tarzan.maxkb4j.core.workflow.logic.LfEdge;
import com.tarzan.maxkb4j.core.workflow.logic.LfNode;
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
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.*;

@Slf4j
@Data
public class Workflow {
    private INode currentNode;
    private ChatParams chatParams;
    private List<LfNode> lfNodes;
    private List<LfEdge> edges;
    private Map<String, Object> context;
    private Map<String, Object> chatContext;
    private List<INode> nodeContext;
    private String answer;
    private ApplicationChatRecordEntity chatRecord;
    private List<ApplicationChatRecordEntity> historyChatRecords;

    public Workflow(List<LfNode> lfNodes, List<LfEdge> edges, ChatParams chatParams, ApplicationChatRecordEntity chatRecord, List<ApplicationChatRecordEntity> historyChatRecords) {
        this.lfNodes = lfNodes;
        this.edges = edges;
        this.chatParams = chatParams;
        this.context = new HashMap<>();
        this.chatContext = new HashMap<>();
        this.nodeContext = new ArrayList<>();
        this.chatRecord = chatRecord;
        this.answer = "";
        this.historyChatRecords = CollectionUtils.isEmpty(historyChatRecords) ? List.of() : historyChatRecords;
        if (StringUtil.isNotBlank(chatParams.getRuntimeNodeId()) && Objects.nonNull(chatRecord)) {
            this.loadNode(chatRecord, chatParams.getRuntimeNodeId(), chatParams.getNodeData());
        }
    }


    @SuppressWarnings("unchecked")
    private void loadNode(ApplicationChatRecordEntity chatRecord, String startNodeId, Map<String, Object> startNodeData) {
        List<Map<String, Object>> sortedDetails = chatRecord.getDetails().values().stream()
                .map(row -> (Map<String, Object>) row)
                .sorted(Comparator.comparingInt(e -> (int) e.get("index")))
                .toList();
        for (Map<String, Object> nodeDetail : sortedDetails) {
            String nodeId = (String) nodeDetail.get("nodeId");
            List<String> upNodeIdList = (List<String>) nodeDetail.get("upNodeIdList");
            String runtimeNodeId = (String) nodeDetail.get("runtimeNodeId");
            if (runtimeNodeId.equals(startNodeId)) {
                // 处理起始节点
                this.currentNode = getNodeClsById(
                        nodeId,
                        upNodeIdList,
                        n -> {
                            JSONObject nodeProperties = n.getProperties();
                            if (nodeProperties.containsKey("nodeData")) {
                                JSONObject nodeParams = nodeProperties.getJSONObject("nodeData");
                                nodeParams.put("form_data", startNodeData);
                            }
                            return nodeProperties;
                        }
                );
                assert currentNode != null;
                currentNode.setDetail(nodeDetail);
                currentNode.saveContext(this, nodeDetail);
                nodeContext.add(currentNode);
            } else {
                // 处理其他节点
                INode node = getNodeClsById(nodeId, upNodeIdList, null);
                assert node != null;
                node.setDetail(nodeDetail);
                node.saveContext(this, nodeDetail);
                nodeContext.add(node);
            }
        }
    }


    public INode getStartNode() {
        return getNodeClsById(START.getKey(), List.of(), null);
    }


    public List<INode> getNextNodeList(INode currentNode, NodeResult currentNodeResult) {
        List<INode> nextNodeList = new ArrayList<>();
        // 判断是否中断执行
        if (currentNodeResult == null || currentNodeResult.isInterruptExec(currentNode)) {
            return nextNodeList;
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
                        processEdge(edge, currentNode, nextNodeList);
                    }
                }
            }
        } else {
            // 处理非断言结果分支
            for (LfEdge edge : edges) {
                if (edge.getSourceNodeId().equals(currentNode.getId())) {
                    processEdge(edge, currentNode, nextNodeList);
                }
            }
        }
        return nextNodeList;
    }


    private void processEdge(LfEdge edge, INode currentNode, List<INode> nodeList) {
        // 查找目标节点
        Optional<LfNode> targetNodeOpt = lfNodes.stream()
                .filter(node -> node.getId().equals(edge.getTargetNodeId()))
                .findFirst();
        if (targetNodeOpt.isEmpty()) {
            return;
        }
        LfNode targetNode = targetNodeOpt.get();
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


    private INode getNodeClsById(String nodeId, List<String> upNodeIds, Function<INode, JSONObject> getNodeProperties) {
        for (LfNode lfNode : lfNodes) {
            if (nodeId.equals(lfNode.getId())) {
                INode node = NodeFactory.getNode(lfNode);
                assert node != null;
                node.setUpNodeIdList(upNodeIds);
                if (getNodeProperties != null) {
                    getNodeProperties.apply(node);
                }
                return node;
            }
        }
        return null;
    }

    private Map<String, Object> getPromptVariables() {
        Map<String, Object> result = new HashMap<>(100);
        for (String key : context.keySet()) {
            result.put("global." + key, context.get(key));
        }
        for (String key : chatContext.keySet()) {
            result.put("chat." + key, chatContext.get(key));
        }
        for (INode node : nodeContext) {
            String nodeName = node.getProperties().getString("nodeName");
            Map<String, Object> context = node.getContext();
            for (String key : context.keySet()) {
                result.put(nodeName + "." + key, context.get(key));
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
            runtimeDetail.put("nodeId", node.getId());
            runtimeDetail.put("upNodeIdList", node.getUpNodeIdList());
            runtimeDetail.put("runtimeNodeId", node.getRuntimeNodeId());
            runtimeDetail.put("name", node.getProperties().getString("nodeName"));
            runtimeDetail.put("index", index);
            runtimeDetail.put("type", node.getType());
            runtimeDetail.put("status", node.getStatus());
            runtimeDetail.put("errMessage", node.getErrMessage());
            runtimeDetail.putAll(node.executeDetail());
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
        boolean defaultVal = !hasNextNode(currentNode, currentNodeResult);
        Boolean isResult = currentNode.getNodeData().getBoolean("isResult");
        return isResult == null ? defaultVal : isResult;
    }

    private boolean dependentNode(String lastNodeId, INode node) {
        if (Objects.equals(lastNodeId, node.getId())) {
            if (FORM.getKey().equals(node.getType()) || USER_SELECT.getKey().equals(node.getType())) {
                Object formData = node.getContext().get("form_data");
                return formData != null;
            }
            return true;
        }
        return false;
    }

    private boolean dependentNodeBeenExecuted(String nodeId) {
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
        return nodeVariable.get(key);
    }


    public String generatePrompt(String prompt) {
        return generatePrompt(prompt,Map.of());
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
