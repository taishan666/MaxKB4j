package com.tarzan.maxkb4j.core.workflow;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.dto.ChatFile;
import com.tarzan.maxkb4j.core.workflow.dto.FlowParams;
import com.tarzan.maxkb4j.core.workflow.logic.LfEdge;
import com.tarzan.maxkb4j.core.workflow.logic.LfNode;
import com.tarzan.maxkb4j.core.workflow.logic.LogicFlow;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.handler.PostResponseHandler;
import com.tarzan.maxkb4j.module.application.vo.ApplicationChatRecordVO;
import com.tarzan.maxkb4j.module.application.vo.ChatMessageVO;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Sinks;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.FORM;
import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.USER_SELECT;

@Slf4j
@Data
public class WorkflowManage {
    private String startNodeId;
    private INode startNode;
    private Map<String, Object> formData;
    private List<ChatFile> imageList;
    private List<ChatFile> documentList;
    private List<ChatFile> audioList;
    private FlowParams flowParams;
    private LogicFlow flow;
    private JSONObject context = new JSONObject();
    private PostResponseHandler postResponseHandler;
    private INode currentNode;
    private NodeResult currentResult;
    private String answer = "";
    private Sinks.Many<ChatMessageVO> sink;
    private ApplicationChatRecordVO chatRecord;
    private List<INode> nodeContext = new ArrayList<>();

    public WorkflowManage(LogicFlow flow, FlowParams flowParams,Sinks.Many<ChatMessageVO> sink, PostResponseHandler postResponseHandler,
                          Map<String, Object> formData, List<ChatFile> imageList,
                          List<ChatFile> documentList, List<ChatFile> audioList, String startNodeId,
                          Map<String, Object> startNodeData, ApplicationChatRecordVO chatRecord) {
        this.formData = formData;
        this.imageList = Objects.requireNonNullElseGet(imageList, ArrayList::new);
        this.documentList = Objects.requireNonNullElseGet(documentList, ArrayList::new);;
        this.audioList = Objects.requireNonNullElseGet(audioList, ArrayList::new);;
        this.flowParams = flowParams;
        this.sink = sink;
        this.flow = flow;
        this.postResponseHandler = postResponseHandler;
        this.chatRecord = chatRecord;
        if (startNodeId != null) {
            this.startNodeId = startNodeId;
            this.loadNode(chatRecord, startNodeId, startNodeData);
        } else {
            this.nodeContext = new ArrayList<>();
        }
    }


    public void loadNode(ApplicationChatRecordEntity chatRecord, String startNodeId, Map<String, Object> startNodeData) {
        nodeContext.clear();
        this.answer = chatRecord.getAnswerText();
        List<JSONObject> sortedDetails = chatRecord.getDetails().values().stream()
                .map(row -> (JSONObject) row)
                .sorted(Comparator.comparingInt(e -> e.getIntValue("index")))
                .toList();
        for (JSONObject nodeDetail : sortedDetails) {
            String nodeId = nodeDetail.getString("node_id");
            if (nodeDetail.getString("runtimeNodeId").equals(startNodeId)) {
                // 处理起始节点
                this.startNode = getNodeClsById(
                        nodeId,
                        (List<String>) nodeDetail.get("up_node_id_list"),
                        n -> {
                            JSONObject params = new JSONObject();
                            boolean isResult = "application-node".equals(n.getType());
                            // 合并节点数据
                            if (n.getProperties().containsKey("nodeData")) {
                                params.putAll(n.getProperties().getJSONObject("nodeData"));
                            }
                            params.put("form_data", startNodeData);
                            params.put("nodeData", startNodeData);
                          //  params.put("child_node", childNode);
                            params.put("isResult", isResult);
                            return params;
                        }
                );
                // 合并验证参数
                assert startNode != null;
                if ("application-node".equals(startNode.getType())) {
                    startNode.getContext().put("application_node_dict", nodeDetail.get("application_node_dict"));
                }
                nodeContext.add(startNode);
                continue;
            }
            // 处理普通节点
            INode node = getNodeClsById(nodeId, (List<String>) nodeDetail.get("up_node_id_list"));
            nodeContext.add(node);
        }
    }

    public String run() {
        context.put("start_time", System.currentTimeMillis());
        String language = "zh";
        runChainManage(startNode, null, language);
        ChatMessageVO vo=new ChatMessageVO(flowParams.getChatId(),flowParams.getChatRecordId(),"",
                true,true);
        sink.tryEmitNext(vo);
        long startTime= context.getLongValue("start_time");
        postResponseHandler.handler(flowParams.getChatId(), flowParams.getChatRecordId(), flowParams.getQuestion(),answer,chatRecord,getRuntimeDetails(),startTime,flowParams.getClientId(),flowParams.getClientType());
        return answer;
    }

    public LfNode getStartNode() {
        return this.flow.getNodes().parallelStream().filter(node -> node.getType().equals("start-node")).findFirst().orElse(null);
    }

    public LfNode getBaseNode() {
        return this.flow.getNodes().parallelStream().filter(node -> node.getType().equals("base-node")).findFirst().orElse(null);
    }

    public void runChainManage(INode currentNode, NodeResultFuture nodeResultFuture, String language) {
        if (currentNode == null) {
            LfNode startNode = getStartNode();
            currentNode = NodeFactory.getNode(startNode.getType(), startNode, flowParams, this);
        }
        NodeResult result = runChainNode(currentNode, nodeResultFuture);
        // 获取下一个节点列表
        List<INode> nodeList = getNextNodeList(currentNode, result);
        if (nodeList.size() == 1) {
            runChainManage(nodeList.get(0), null, language);
        } else if (nodeList.size() > 1) {
            // 提交子任务并获取Future对象
            for (INode node : nodeList) {
                runChainManage(node, null, language);
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
            for (LfEdge edge : flow.getEdges()) {
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
            for (LfEdge edge : flow.getEdges()) {
                if (edge.getSourceNodeId().equals(currentNode.getId())) {
                    processEdge(edge, currentNode, nodeList);
                }
            }
        }
        return nodeList;
    }


    private void processEdge(LfEdge edge, INode currentNode, List<INode> nodeList) {
        // 查找目标节点
        Optional<LfNode> targetNodeOpt = flow.getNodes().stream()
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
        if (currentNode.getLastNodeIdList() != null) {
            newUpNodeIds.addAll(currentNode.getLastNodeIdList());
        }
        newUpNodeIds.add(currentNode.getLfNode().getId());
        // 获取节点实例并添加到列表
        INode nextNode = getNodeClsById(targetNodeId, newUpNodeIds);
        if (nextNode != null) {
            nodeList.add(nextNode);
        }
    }

    private INode getNodeClsById(String targetNodeId, List<String> newUpNodeIds) {
        return getNodeClsById(targetNodeId, newUpNodeIds, null);
    }

    private INode getNodeClsById(String nodeId, List<String> lastNodeIds, Function<LfNode, JSONObject> getNodeParams) {
        for (LfNode node : this.flow.getNodes()) {
            if (nodeId.equals(node.getId())) {
                return NodeFactory.getNode(node.getType(), node, flowParams, this, lastNodeIds, getNodeParams);
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
        try {
            NodeResult currentResult = nodeResultFuture.getResult();
            currentResult.writeContext(currentNode, this);
            return currentResult;
        } catch (Exception e) {
            log.error("runChain error: {}",e.getMessage());
        }
        return null;
    }


    public String generatePrompt(String prompt) {
        prompt = prompt == null ? "" : prompt;
        if (StringUtils.isNotBlank(prompt)) {
            prompt = this.resetPrompt(prompt);
            Set<String> promptVariables = extractVariables(prompt);
            Map<String, Object> context = this.getWorkflowContent();
            Map<String, Object> variables = flattenMap(context);
            for (String promptVariable : promptVariables) {
                if (!variables.containsKey(promptVariable)) {
                    variables.put(promptVariable, "");
                }
            }
            PromptTemplate promptTemplate = PromptTemplate.from(prompt);
            return promptTemplate.apply(variables).text();
        }
        return prompt;
    }

    public Map<String, Object> flattenMap(Map<String, Object> inputMap) {
        Map<String, Object> result = new HashMap<>();
        flattenMapHelper("", inputMap, result);
        return result;
    }

    private void flattenMapHelper(String parentKey, Map<String, Object> inputMap, Map<String, Object> result) {
        for (Map.Entry<String, Object> entry : inputMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String newKey = parentKey.isEmpty() ? key : parentKey + "." + key;
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> valueMap = (Map<String, Object>) value;
                flattenMapHelper(newKey, valueMap, result);
            } else {
                result.put(newKey, value);
            }
        }
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

    public Map<String, Object> getWorkflowContent() {
        JSONObject workflowContext = new JSONObject();
        workflowContext.put("global", context);
        for (INode node : nodeContext) {
            workflowContext.put(node.getId(), node.getContext());
        }
        return jsonToMap(workflowContext);
    }

    // 重置提示词的方法
    public String resetPrompt(String prompt) {
        for (LfNode node : flow.getNodes()) { // 假设getNodes()返回节点列表
            JSONObject properties = node.getProperties();
            JSONObject nodeConfig = properties.getJSONObject("config");
            if (nodeConfig != null) {
                JSONArray fields = nodeConfig.getJSONArray("fields");
                if (fields != null) {
                    for (int i = 0; i < fields.size(); i++) {
                        JSONObject field = fields.getJSONObject(i);
                        String globeLabel = properties.getString("stepName") + "." + field.getString("value");
                        String globeValue = node.getId() + "." + field.getString("value");
                        prompt = prompt.replace(globeLabel, globeValue);
                    }
                }
                JSONArray globalFields = nodeConfig.getJSONArray("globalFields");
                if (globalFields != null) {
                    for (int i = 0; i < globalFields.size(); i++) {
                        JSONObject globalField = globalFields.getJSONObject(i);
                        String globeLabel = "全局变量." + globalField.getString("value");
                        String globeValue = "global." + globalField.getString("value");
                        prompt = prompt.replace(globeLabel, globeValue);
                    }
                }
            }
        }
        return prompt;
    }

    public JSONObject getRuntimeDetails() {
        JSONObject detailsResult = new JSONObject();
        if (nodeContext == null || nodeContext.isEmpty()) {
            return detailsResult;
        }
        for (int index = 0; index < nodeContext.size(); index++) {
            INode node = nodeContext.get(index);
            JSONObject details;
            if (chatRecord != null && chatRecord.getDetails() != null) {
                details = chatRecord.getDetails().getJSONObject(node.getRuntimeNodeId());
                if (details != null &&startNode != null && !startNode.getRuntimeNodeId().equals(node.getRuntimeNodeId())) {
                    detailsResult.put(node.getRuntimeNodeId(), details);
                    continue;
                }
            }
            details = node.getDetail(index);
            details.put("node_id", node.getId());
            details.put("up_node_id_list", node.getLastNodeIdList());
            details.put("runtimeNodeId", node.getRuntimeNodeId());
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
            //  node.validArgs(node.nodeParams, node.workflowParams);
            node.setWorkflowManage(this);
            NodeResult result = node.run();
            return new NodeResultFuture(result, null, 200);
        } catch (Exception ex) {
            log.error(node.getType());
            log.error(ex.getMessage());
            return new NodeResultFuture(null, ex, 500);
        }
    }

    private boolean hasNextNode(INode currentNode, NodeResult nodeResult) {
        if (nodeResult != null && nodeResult.isAssertionResult()) {
            for (LfEdge edge : flow.getEdges()) {
                if (edge.getSourceNodeId().equals(currentNode.getId())) {
                    String branchId = (String) nodeResult.getNodeVariable().get("branch_id");
                    String expectedSourceAnchorId = String.format("%s_%s_right", edge.getSourceNodeId(), branchId);
                    if (expectedSourceAnchorId.equals(edge.getSourceAnchorId())) {
                        return true;
                    }
                }
            }
        } else {
            for (LfEdge edge : flow.getEdges()) {
                if (edge.getSourceNodeId().equals(currentNode.getId())) {
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
        Boolean isResult = currentNode.getNodeParams().getBoolean("isResult");
        return isResult == null ? defaultVal : isResult;
    }

    public boolean dependentNode(String lastNodeId, INode node) {
        if (Objects.equals(lastNodeId, node.id)) {
            if (FORM.getKey().equals(node.type)||USER_SELECT.getKey().equals(node.type)) {
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
        for (LfEdge edge : this.flow.getEdges()) {
            if (edge.getTargetNodeId().equals(nodeId)) {
                upNodeIdList.add(edge.getSourceNodeId());
            }
        }
        // 检查每个上游节点是否都已执行
        return upNodeIdList.stream().allMatch(upNodeId ->
                this.nodeContext.stream().anyMatch(node -> dependentNode(upNodeId, node))
        );
    }

    public Object getFieldValue(String fieldType, Object value, List<String> reference) {
        if ("referencing".equals(fieldType)) {
            return getReferenceField(reference.get(0), reference.subList(1, reference.size()));
        } else {
            return value;
        }
    }

    public Object getReferenceField(String nodeId, List<String> fields) {
        if ("global".equals(nodeId)) {
            return INode.getField(this.context, fields.get(0));
        } else {
            INode node = this.getNodeById(nodeId);
            return node==null?null:node.getReferenceField(fields.get(0));
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


    public List<ChatMessage> getHistoryMessage(List<ApplicationChatRecordEntity> historyChatRecord, int dialogueNumber, String dialogueType, String runtimeNodeId) {
        List<ChatMessage> historyMessage = new ArrayList<>();
        int startIndex = Math.max(historyChatRecord.size() - dialogueNumber, 0);
        // 遍历指定范围内的聊天记录
        for (int index = startIndex; index < historyChatRecord.size(); index++) {
            // 获取每条消息并添加到历史消息列表中
            historyMessage.addAll(getMessage(historyChatRecord.get(index), dialogueType, runtimeNodeId));
        }
        // 使用Stream API和flatMap来代替Python中的reduce操作
        return historyMessage;
    }

    private List<ChatMessage> getMessage(ApplicationChatRecordEntity chatRecord, String dialogueType, String runtimeNodeId) {
        if ("NODE".equals(dialogueType)) {
            return getNodeMessage(chatRecord, runtimeNodeId);
        } else {
            return getNodeMessage(chatRecord);
        }
    }

    public List<ChatMessage> getNodeMessage(ApplicationChatRecordEntity chatRecord) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new UserMessage(chatRecord.getProblemText()));
        messages.add(new AiMessage(chatRecord.getAnswerText()));
        return messages;
    }

    public List<ChatMessage> getNodeMessage(ApplicationChatRecordEntity chatRecord, String runtimeNodeId) {
        // 获取节点详情
        JSONObject nodeDetails = chatRecord.getNodeDetailsByRuntimeNodeId(runtimeNodeId);
        // 如果节点详情为空，返回空列表
        if (nodeDetails == null) {
            return new ArrayList<>();
        }
        // 创建消息列表
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new UserMessage(nodeDetails.getString("question")));
        messages.add(new AiMessage(nodeDetails.getString("answer")));
        return messages;
    }


}
