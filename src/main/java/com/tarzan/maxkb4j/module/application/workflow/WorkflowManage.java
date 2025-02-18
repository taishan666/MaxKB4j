package com.tarzan.maxkb4j.module.application.workflow;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.vo.ApplicationChatRecordVO;
import com.tarzan.maxkb4j.module.application.workflow.dto.Answer;
import com.tarzan.maxkb4j.module.application.workflow.dto.BaseToResponse;
import com.tarzan.maxkb4j.module.application.workflow.dto.ChunkInfo;
import com.tarzan.maxkb4j.module.application.workflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.workflow.handler.WorkFlowPostHandler;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

@Slf4j
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
    private JSONObject context = new JSONObject();
    private NodeChunkManage nodeChunkManage;
    private WorkFlowPostHandler workFlowPostHandler;
    private INode currentNode;
    private NodeResult currentResult;
    private String answer = "";
    private List<String> answerList = new ArrayList<>(Collections.singletonList(""));
    private int status = 200;
    private BaseToResponse baseToResponse;
    private ApplicationChatRecordVO chatRecord;
    private Map<String, CompletableFuture<?>> awaitFutureMap = new HashMap<>();
    private Object childNode; // 根据实际需要定义类型
    private final List<Future<?>> futureList = new ArrayList<>();
    private List<INode> nodeContext = new ArrayList<>();
    private ExecutorService executorService = Executors.newFixedThreadPool(5);

    public WorkflowManage(Flow flow, FlowParams params, WorkFlowPostHandler workFlowPostHandler,
                          BaseToResponse baseToResponse, Map<String, Object> formData, List<Object> imageList,
                          List<Object> documentList, List<Object> audioList, String startNodeId,
                          Map<String, Object> startNodeData, ApplicationChatRecordVO chatRecord, Object childNode) {
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
        if (startNodeId != null) {
            this.startNodeId = startNodeId;
            this.loadNode(chatRecord, startNodeId, startNodeData);
        } else {
            this.nodeContext = new ArrayList<>();
        }
        this.params = params;
        this.flow = flow;
        this.workFlowPostHandler = workFlowPostHandler;
        this.baseToResponse = baseToResponse;
        this.chatRecord = chatRecord;
        this.childNode = childNode;
        this.nodeChunkManage = new NodeChunkManage(this);
    }

    public boolean answerIsNotEmpty() {
        if (answerList == null || answerList.isEmpty()) {
            return false;
        }
        String lastAnswer = answerList.get(answerList.size() - 1);
        return lastAnswer != null && !lastAnswer.trim().isEmpty();
    }


    public void appendAnswer(String content) {
        this.answer += content;
        this.answerList.add(content);
    }

    public List<String> getAnswerTextList() {
        List<Answer> answerList = nodeContext.stream()
                .map(INode::getAnswerList)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .toList();

        List<String> result = new ArrayList<>();
        Answer upNode = null;

        for (Answer currentAnswer : answerList) {
            if (!currentAnswer.getContent().isEmpty()) {
                if (upNode == null || "single_view".equals(currentAnswer.getViewType()) ||
                        ("many_view".equals(currentAnswer.getViewType()) && "single_view".equals(upNode.getViewType()))) {
                    result.add(currentAnswer.getContent());
                } else {
                    if (!result.isEmpty()) {
                        int execIndex = result.size() - 1;
                        String content = result.get(execIndex);
                        result.set(execIndex, (content.isEmpty() ? currentAnswer.getContent() : content + "\n\n" + currentAnswer.getContent()));
                    } else {
                        result.add(0, currentAnswer.getContent());
                    }
                }
                upNode = currentAnswer;
            }
        }

        if (result.isEmpty()) {
            // 如果没有响应 就响应一个空数据
            return Collections.singletonList("");
        }

        return result;
    }

    public void loadNode(ApplicationChatRecordEntity chatRecord, String startNodeId, Map<String, Object> startNodeData) {
        nodeContext.clear();
        this.answer = chatRecord.getAnswerText();
        this.answerList = new ArrayList<>(chatRecord.getAnswerTextList());
        this.answerList.add("");
        List<JSONObject> sortedDetails = chatRecord.getDetails().values().stream()
                .map(row -> (JSONObject) row)
                .sorted(Comparator.comparingInt(e->e.getIntValue("index")))
                .toList();

        for (JSONObject nodeDetail : sortedDetails) {
            String nodeId = nodeDetail.getString("node_id");
            if (nodeDetail.getString("runtime_node_id").equals(startNodeId)) {
                // 处理起始节点
                this.startNode = getNodeClsById(
                        nodeId,
                        (List<String>) nodeDetail.get("up_node_id_list"),
                        n -> {
                            JSONObject params = new JSONObject();
                            boolean isResult = "application-node".equals(n.getType());

                            // 合并节点数据
                            if (n.getProperties().containsKey("node_data")) {
                                params.putAll(n.getProperties().getJSONObject("node_data"));
                            }

                            params.put("form_data", startNodeData);
                            params.put("node_data", startNodeData);
                            params.put("child_node", childNode); // 假设childNode方法存在
                            params.put("is_result", isResult);
                            return params;
                        }
                );

                // 合并验证参数
                assert startNode != null;
                JSONObject validationParams = startNode.getNodeParams();
                validationParams.put("form_data", startNodeData);
                try {
                    startNode.validArgs(validationParams, startNode.getWorkflowParams());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                if ("application-node".equals(startNode.getType())) {
                    startNode.getContext().put("application_node_dict", nodeDetail.get("application_node_dict"));
                }

                nodeContext.add(startNode);
                continue;
            }

            // 处理普通节点
            INode node = getNodeClsById(nodeId,(List<String>) nodeDetail.get("up_node_id_list"));
            try {
                node.validArgs(node.getNodeParams(), node.getWorkflowParams());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            node.saveContext(nodeDetail, this);
            nodeContext.add(node);
        }
    }

    public Flux<JSONObject> run() {
        //   closeOldConnections();
        //  String language = getLanguage();
        context.put("start_time", System.currentTimeMillis());
        String language = "zh";
        if (params.getStream()) {
            return runStream(startNode, null, language);
        } else {
            return runBlock(language);
        }
    }


    public Flux<JSONObject> runStream(INode currentNode, NodeResultFuture nodeResultFuture, String language) {
        runChainAsync(currentNode, nodeResultFuture, language);
        return awaitResult();
    }

    public Flux<JSONObject> awaitResult() {
        NodeChunkManage nodeChunkManage = this.nodeChunkManage;
        WorkflowManage workflow = this;

        // 使用 Sinks.Many 来创建一个事件驱动的异步流
        Sinks.Many<JSONObject> sink = Sinks.many().multicast().onBackpressureBuffer();

        // 启动一个异步任务来监听 nodeChunkManage 的变化
        executorService.submit(() -> {
            try {
                while (isRun()) {
                    JSONObject chunk = nodeChunkManage.pop();
                    if (chunk != null) {
                        // 将数据发送到 Sinks
                        sink.tryEmitNext(chunk);
                    }
                }
                workFlowPostHandler.handler(this.params.getChatId(), this.params.getChatRecordId(),
                        answer, workflow);
                sink.tryEmitComplete();
            } catch (Exception e) {
                sink.tryEmitError(e);
            }
        });

        // 返回由 Sinks 转换为 Flux 的流
        return sink.asFlux();
    }


    public void runChainAsync(INode currentNode, NodeResultFuture nodeResultFuture, String language) {
        // 提交任务给线程池执行，并获取对应的Future对象
        Future<?> future = executorService.submit(() -> {
            try {
                runChainManage(currentNode, nodeResultFuture, language);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        // 将Future对象添加到futureList列表中
        futureList.add(future);
    }


    public Node getStartNode() {
        return this.flow.getNodes().parallelStream().filter(node -> node.getType().equals("start-node")).findFirst().orElse(null);
    }

    public Node getBaseNode() {
        return this.flow.getNodes().parallelStream().filter(node -> node.getType().equals("start-node")).findFirst().orElse(null);
    }

    public void runChainManage(INode currentNode, NodeResultFuture nodeResultFuture, String language) throws InterruptedException, ExecutionException {
        System.out.println("runChainManage");
        // 激活翻译（假设有一个类似功能的类）
        // Translation.activate(language);
        if (currentNode == null) {
            Node startNode = getStartNode();
            currentNode = NodeFactory.getNode(startNode.getType(), startNode, params, this);
        }

        assert currentNode != null;
        // 添加节点块
        nodeChunkManage.addNodeChunk(currentNode.getNodeChunk());
        // 添加节点
        appendNode(currentNode);
        // 执行链式任务
        NodeResult result = runChain(currentNode, nodeResultFuture);
        if (result == null) {
            return;
        }

        // 获取下一个节点列表
        List<INode> nodeList = getNextNodeList(currentNode, result);
        if (nodeList.size() == 1) {
            System.out.println("node getType=" + nodeList.get(0).getType());
            runChainManage(nodeList.get(0), null, language);
        } else if (nodeList.size() > 1) {
            // 对节点进行排序
            List<INode> sortedNodeRunList = nodeList.stream()
                    .sorted(Comparator.comparingInt(n -> n.getNode().getY()))
                    .toList();
            // 提交子任务并获取Future对象
            for (INode node : sortedNodeRunList) {
                Future<?> future = executorService.submit(() -> {
                    try {
                        runChainManage(node, null, language);
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                });
                synchronized (futureList) {
                    futureList.add(future);
                }
            }
        }
    }


    public List<INode> getNextNodeList(INode currentNode, NodeResult currentNodeResult) {
        List<INode> nodeList = new ArrayList<>();
        // 判断是否中断执行
        /*if (currentNodeResult != null && currentNodeResult.isInterruptExec(currentNode)) {
            return nodeList;
        }*/
        if (currentNodeResult != null && currentNodeResult.isAssertionResult()) {
            // 处理断言结果分支
            for (Edge edge : flow.getEdges()) {

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
            for (Edge edge : flow.getEdges()) {
                if (edge.getSourceNodeId().equals(currentNode.getId())) {
                    processEdge(edge, currentNode, nodeList);
                }
            }
        }
        return nodeList;
    }


    private void processEdge(Edge edge, INode currentNode, List<INode> nodeList) {
        // 查找目标节点
        Optional<Node> targetNodeOpt = flow.getNodes().stream()
                .filter(node -> node.getId().equals(edge.getTargetNodeId()))
                .findFirst();
        if (targetNodeOpt.isEmpty()) {
            return;
        }
        Node targetNode = targetNodeOpt.get();
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
        newUpNodeIds.add(currentNode.getNode().getId());

        // 获取节点实例并添加到列表
        INode nextNode = getNodeClsById(targetNodeId, newUpNodeIds);
        if (nextNode != null) {
            nodeList.add(nextNode);
        }
    }

    private INode getNodeClsById(String targetNodeId, List<String> newUpNodeIds) {
        return getNodeClsById(targetNodeId, newUpNodeIds, null);
    }

    private INode getNodeClsById(String nodeId, List<String> lastNodeIds,
                                 Function<Node, JSONObject> getNodeParams) {
        for (Node node : this.flow.getNodes()) {
            if (nodeId.equals(node.getId())) {
                return NodeFactory.getNode(node.getType(), node, params, this, lastNodeIds, getNodeParams);
            }
        }
        return null;
    }


    public NodeResult runChain(INode currentNode, NodeResultFuture nodeResultFuture) {
        // 处理默认的nodeResultFuture
        if (nodeResultFuture == null) {
            nodeResultFuture = runNodeFuture(currentNode);
        }
        try {
            // 获取stream参数并处理默认值
            boolean isStream = params.getStream();
            // 根据流模式选择处理方法
            NodeResult result;
            if (isStream) {
                result = handEventNodeResult(currentNode, nodeResultFuture);
            } else {
                result = handleNodeResult(currentNode, nodeResultFuture);
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public NodeResult handEventNodeResult(INode currentNode, NodeResultFuture nodeResultFuture) {
        String realNodeId = currentNode.getRuntimeNodeId();
        Map<String, Object> child_node = new HashMap<>();
        String view_type = currentNode.getViewType();
        NodeResult currentResult = nodeResultFuture.getResult();
        try {
            Object result = currentResult.writeContext(currentNode, this);
            if (result != null) {
                if (isResult(currentNode, currentResult)) {
                    String content = "";
                    if (result instanceof Iterator) {
                        Iterator<String> iterator = (Iterator<String>) result;
                        while (iterator.hasNext()) {
                            content = iterator.next();
                            JSONObject chunk = this.getBaseToResponse().toStreamChunkResponse(getParams().getChatId(),
                                    getParams().getChatRecordId(),
                                    currentNode.getId(),
                                    currentNode.getLastNodeIdList(),
                                    content,
                                    false, 0, 0,
                                    new ChunkInfo(currentNode.getType(),
                                            currentNode.runtimeNodeId,
                                            view_type,
                                            child_node,
                                            false,
                                            realNodeId));
                            currentNode.getNodeChunk().addChunk(chunk);
                        }
                    }

                    JSONObject endChunk = this.getBaseToResponse().toStreamChunkResponse(getParams().getChatId(),
                            getParams().getChatRecordId(),
                            currentNode.getId(),
                            currentNode.getLastNodeIdList(),
                            "", false, 0, 0,
                            new ChunkInfo(currentNode.getType(),
                                    realNodeId,
                                    view_type,
                                    child_node,
                                    true,
                                    realNodeId));
                    currentNode.getNodeChunk().addChunk(endChunk);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("异常=" + e.getMessage());
            JSONObject errorChunk = this.getBaseToResponse().toStreamChunkResponse(getParams().getChatId(),
                    getParams().getChatRecordId(),
                    currentNode.getId(),
                    currentNode.getLastNodeIdList(),
                    e.getMessage(), false, 0, 0,
                    new ChunkInfo(currentNode.getType(),
                            currentNode.getRuntimeNodeId(),
                            currentNode.getViewType(),
                            new HashMap<>(),
                            true,
                            realNodeId));
            currentNode.getNodeChunk().addChunk(errorChunk);
            currentNode.getWriteErrorContext(e);
            this.status = 500;
        } finally {
            currentNode.getNodeChunk().end(null);
        }
        return currentResult;
    }

    public NodeResult handleNodeResult(INode currentNode, NodeResultFuture nodeResultFuture) {
        return null;
    }


    public Flux<JSONObject> runBlock(String language) {
        if (language == null || language.isEmpty()) {
            language = "zh";
        }
        runChainAsync(null, null, language);
        List<String> answerTextList = getAnswerTextList();
        StringBuilder answerText = new StringBuilder();
        for (String answer : answerTextList) {
            if (!answerText.isEmpty()) {
                answerText.append("\n\n");
            }
            answerText.append(answer);
        }

        String chatId = params.getChatId();
        String chatRecordId = params.getChatRecordId();

       // workFlowPostHandler.handler(chatId, chatRecordId, answerText.toString(), this);

        baseToResponse.toBlockResponse(chatId,
                chatRecordId,
                answerText.toString(),
                true,
                0,
                0,
                getStatus());
        return Flux.just(new JSONObject());
    }


    public boolean isRun() {
        List<Boolean> list = new ArrayList<>();
        for (Future<?> future : futureList) {
            list.add(future.isDone());
        }
        boolean flag = true;
        for (Boolean b : list) {
            flag = flag && b;
        }
        return !flag;
    }

    public boolean isRun1(long timeout, TimeUnit timeUnit) {
        int futureListLen = futureList.size();
        System.out.println("futureListLen=" + futureListLen);
        // 创建两个列表用于存储已完成和未完成的任务
        List<Future<?>> done = new ArrayList<>();
        List<Future<?>> notDone = new ArrayList<>(futureList);

        long endTime = System.nanoTime() + timeUnit.toNanos(timeout);
        // 遍历所有Future对象并检查它们的状态
        for (Future<?> future : notDone) {
            try {
                // 使用带超时的get方法来等待任务完成
                future.get(endTime - System.nanoTime(), TimeUnit.NANOSECONDS);
                done.add(future);
            } catch (TimeoutException e) {
                // 超时异常处理，任务未完成
                break;
            } catch (InterruptedException | ExecutionException e) {
                // 其他异常处理
                Thread.currentThread().interrupt(); // 恢复中断状态
                return true; // 发生异常时视为任务仍在运行
            }
        }

        notDone.removeAll(done); // 更新未完成的任务列表

        if (!notDone.isEmpty()) {
            return true; // 有未完成的任务
        } else {
            // futureList长度发生变化
            return futureListLen != futureList.size(); // 没有未完成的任务且futureList长度没有变化
        }
    }


    public String generatePrompt(String prompt) {
        prompt = prompt==null?"":prompt;
        if(StringUtils.isNotBlank(prompt)){
            prompt = this.resetPrompt(prompt);
            PromptTemplate promptTemplate = PromptTemplate.from(prompt);
            Map<String, Object> context = this.getWorkflowContent();
            return promptTemplate.apply(context).text();
        }
        return prompt;
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
        for (Node node : flow.getNodes()) { // 假设getNodes()返回节点列表
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
                if (details != null && !startNode.getRuntimeNodeId().equals(node.getRuntimeNodeId())) {
                    detailsResult.put(node.getRuntimeNodeId(), details);
                    continue;
                }
            }

            details = node.getDetail(index);
            details.put("node_id", node.getId());
            details.put("up_node_id_list", node.getLastNodeIdList());
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
            //  node.validArgs(node.nodeParams, node.workflowParams);
            node.setWorkflowManage(this);
            NodeResult result = node.run();
            return new NodeResultFuture(result, null, 200);
        } catch (Exception ex) {
            return new NodeResultFuture(null, ex, 500);
        }
    }

    private boolean hasNextNode(INode currentNode, NodeResult nodeResult) {
        if (nodeResult != null && nodeResult.isAssertionResult()) {
            for (Edge edge : flow.getEdges()) {
                if (edge.getSourceNodeId().equals(currentNode.getId())) {
                    String branchId = (String) nodeResult.getNodeVariable().get("branch_id");
                    String expectedSourceAnchorId = String.format("%s_%s_right", edge.getSourceNodeId(), branchId);
                    if (expectedSourceAnchorId.equals(edge.getSourceAnchorId())) {
                        return true;
                    }
                }
            }
        } else {
            for (Edge edge : flow.getEdges()) {
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
        Boolean isResult = currentNode.getNodeParams().getBoolean("is_result");
        return isResult == null ? defaultVal : isResult;
    }

    public boolean dependentNode(String lastNodeId, INode node) {
        if (Objects.equals(lastNodeId, node.id)) {
            if ("form-node".equals(node.type)) {
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

    public Object getReferenceField(String nodeId, List<String> fields) {
        if ("global".equals(nodeId)) {
            return INode.getField(this.context, fields.get(0));
        } else {
            return this.getNodeById(nodeId).getReferenceField(fields.get(0));
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


}
