package com.tarzan.maxkb4j.core.workflow;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.tarzan.maxkb4j.core.workflow.domain.ChatRecordSimple;
import com.tarzan.maxkb4j.core.workflow.logic.LfEdge;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.chat.ChatParams;
import com.tarzan.maxkb4j.util.StringUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.*;

@Slf4j
@Data
public class WorkflowManage {
    private INode startNode;
    private ChatParams chatParams;
    private List<INode> nodes;
    private List<LfEdge> edges;
    private JSONObject context;
    private String answer;
    private Sinks.Many<ChatMessageVO> sink;
    private ApplicationChatRecordEntity chatRecord;
    private List<ApplicationChatRecordEntity> historyChatRecords;
    private List<INode> nodeContext;

    public WorkflowManage(List<INode> nodes, List<LfEdge> edges, ChatParams chatParams, ApplicationChatRecordEntity chatRecord, List<ApplicationChatRecordEntity> historyChatRecords) {
        this.nodes = nodes;
        this.edges = edges;
        this.chatParams = chatParams;
        this.context = new JSONObject();
        this.nodeContext = new ArrayList<>();
        this.sink = chatParams.getSink();
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
                            JSONObject properties = n.getProperties();
                            boolean isResult = APPLICATION.name().equals(n.getType());
                            // 合并节点数据
                            if (properties.containsKey("nodeData")) {
                                JSONObject nodeData = properties.getJSONObject("nodeData");
                                nodeData.put("form_data", startNodeData);
                                //  nodeData.put("child_node", childNode);
                                nodeData.put("isResult", isResult);
                                properties.put("nodeData", nodeData);
                            }
                            return properties;
                        }
                );
                // 合并验证参数
                assert startNode != null;
                if ("application-node".equals(startNode.getType())) {
                    startNode.getContext().put("application_node_dict", nodeDetail.get("application_node_dict"));
                }
                startNode.setContext(nodeDetail);
                nodeContext.add(startNode);
            } else {
                // 处理普通节点
                INode node = getNodeClsById(nodeId, lastNodeIdList, null);
                assert node != null;
                node.setContext(nodeDetail);
                nodeContext.add(node);
            }
        }
        Map<String, Object> globalVariable = getGlobalVariable(chatParams);
        context.putAll(globalVariable);
    }


    public Map<String, Object> getGlobalVariable(ChatParams chatParams) {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        resultMap.put("history_context", getHistoryContext());
        resultMap.put("chatId", chatParams.getChatId());
        resultMap.put("chat_user_id", IdWorker.get32UUID());
        resultMap.put("chat_user_type", "ANONYMOUS_USER");
        resultMap.put("chat_user", new JSONObject(Map.of("username", "游客")));
        return resultMap;
    }

    //todo 优化
    public String getHistoryContext() {
        // 获取历史聊天记录
        List<ChatRecordSimple> historyContext = new ArrayList<>();
        for (ApplicationChatRecordEntity chatRecord : historyChatRecords) {
            ChatRecordSimple record = new ChatRecordSimple();
            record.setQuestion(chatRecord.getProblemText());
            record.setAnswer(chatRecord.getAnswerText());
            historyContext.add(record);
        }
        return JSON.toJSONString(historyContext);
    }

    public String run() {
        runChainManage(startNode, null);
        ChatMessageVO vo = new ChatMessageVO(chatParams.getChatId(), chatParams.getChatRecordId(), true);
        sink.tryEmitNext(vo);
        return answer;
    }


    public INode getStartNode() {
        return getNodeClsById(START.getKey(), List.of(), null);
    }

/*    public INode getBaseNode() {
        return this.nodes.parallelStream().filter(node -> node.getType().equals("base-node")).findFirst().orElse(null);
    }*/

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
                    String branchId = nodeVariables != null ? (String) nodeVariables.getOrDefault("branch_id", "") : "";
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


    private INode getNodeClsById(String nodeId, List<String> upNodeIds, Function<INode, JSONObject> getNodeProperties) {
        for (INode node : nodes) {
            if (nodeId.equals(node.getId())) {
                node.setWorkflowManage(this);
                node.setUpNodeIdList(upNodeIds);
                if (getNodeProperties != null) {
                    node.setProperties(getNodeProperties.apply(node));
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


    public String generatePrompt(String prompt) {
        if (StringUtils.isBlank(prompt)) {
            return "";
        }
        Set<String> promptVariables = extractVariables(prompt);
        if (!promptVariables.isEmpty()) {
            Map<String, Object> allVariables = this.allVariables();
            Map<String, Object> variables = new HashMap<>();
            for (String promptVariable : promptVariables) {
                variables.put(promptVariable, allVariables.getOrDefault(promptVariable, "*"));
            }
            PromptTemplate promptTemplate = PromptTemplate.from(prompt);
            return promptTemplate.apply(variables).text();
        }
        return prompt;
    }


    private static Set<String> extractVariables(String template) {
        Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(.+?)\\}\\}");
        Set<String> variables = new HashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        return variables;
    }

    public UserMessage generatePromptQuestion(String prompt) {
        return UserMessage.from(this.generatePrompt(prompt));
    }

    public List<ChatMessage> generateMessageList(String system, UserMessage question, List<ChatMessage> historyMessages) {
        List<ChatMessage> messageList = new ArrayList<>();
        if (StringUtils.isNotBlank(system)) {
            messageList.add(SystemMessage.from(system));
        }
        messageList.addAll(historyMessages);
        messageList.add(question);
        return messageList;
    }


    public Map<String, Object> jsonToMap(JSONObject jsonObject) {
        Map<String, Object> resultMap = new HashMap<>();
        iteratorJson("", jsonObject, resultMap);
        return resultMap;
    }

    private void iteratorJson(String parentKey, JSONObject json, Map<String, Object> resultMap) {
        Set<String> keys = json.keySet();
        for (String key : keys) {
            Object value = json.get(key);
            // 生成新的key，如果parentKey不为空，则使用parentKey.key格式
            String newKey = (!parentKey.isEmpty()) ? (parentKey + "." + key) : key;
            if (value instanceof JSONObject) {
                // 如果是JSONObject，递归处理
                iteratorJson(newKey, (JSONObject) value, resultMap);
            } else {
                // 否则，直接放入结果map中
                resultMap.put(newKey, Objects.requireNonNullElse(value, ""));
            }
        }
    }

    public Map<String, Object> allVariables() {
        JSONObject workflowContext = new JSONObject();
        workflowContext.put("global", context);
        for (INode node : nodeContext) {
            String nodeName=node.getProperties().getString("nodeName");
            workflowContext.put(nodeName, node.getContext());
        }
        return jsonToMap(workflowContext);
    }

    public JSONObject getRuntimeDetails() {
        JSONObject detailsResult = new JSONObject();
        if (nodeContext == null || nodeContext.isEmpty()) {
            return detailsResult;
        }
        for (int index = 0; index < nodeContext.size(); index++) {
            INode node = nodeContext.get(index);
            JSONObject details= node.getDetail(index);
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
            if (currentNode.id.equals(node.id) && currentNode.runtimeNodeId.equals(node.runtimeNodeId)) {
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
            log.error("{} ERROR :{}",node.getType(),ex.getMessage());
            return new NodeResultFuture(null, ex, 500);
        }
    }

    private boolean hasNextNode(INode currentNode, NodeResult nodeResult) {
        if (nodeResult != null && nodeResult.isAssertionResult()) {
            for (LfEdge edge : edges) {
                if (edge.getSourceNodeId().equals(currentNode.getId())) {
                    String branchId = (String) nodeResult.getNodeVariable().get("branch_id");
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
        if (Objects.equals(lastNodeId, node.id)) {
            if (FORM.getKey().equals(node.type) || USER_SELECT.getKey().equals(node.type)) {
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


    public Object getReferenceField(String nodeId, String key) {
        if ("global".equals(nodeId)) {
            return context.get(key);
        } else {
            INode node = this.getNodeById(nodeId);
            return node == null ? null : node.getReferenceField(key);
        }
    }

    public INode getNodeById(String nodeId) {
        for (INode node : this.nodeContext) {
            if (node.getId().equals(nodeId)) {
                return node;
            }
        }
        return null;
    }


    public List<ChatMessage> getHistoryMessages(int dialogueNumber, String dialogueType, String runtimeNodeId) {
        List<ChatMessage> historyMessages;
        if("NODE".equals(dialogueType)){
            historyMessages=getNodeMessages(runtimeNodeId);
        }else {
            historyMessages=getWorkFlowMessages();
        }
        int total=historyMessages.size();
        if (total==0){
            return historyMessages;
        }
        int startIndex = Math.max(total - dialogueNumber*2, 0);
        return historyMessages.subList(startIndex, total);
    }


    public List<ChatMessage> getWorkFlowMessages() {
        List<ChatMessage> messages = new ArrayList<>();
        for (ApplicationChatRecordEntity message : historyChatRecords) {
            messages.add(new UserMessage(message.getProblemText()));
            messages.add(new AiMessage(message.getAnswerText()));
        }
        return messages;
    }

    public List<ChatMessage> getNodeMessages(String runtimeNodeId) {
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
