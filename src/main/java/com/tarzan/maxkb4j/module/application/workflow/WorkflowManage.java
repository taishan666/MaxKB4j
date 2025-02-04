package com.tarzan.maxkb4j.module.application.workflow;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.vo.ApplicationChatRecordVO;
import com.tarzan.maxkb4j.module.application.workflow.dto.Answer;
import com.tarzan.maxkb4j.module.application.workflow.dto.BaseToResponse;
import com.tarzan.maxkb4j.module.application.workflow.dto.ChunkInfo;
import com.tarzan.maxkb4j.module.application.workflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.workflow.handler.WorkFlowPostHandler;
import lombok.Data;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private JSONObject context = new JSONObject();
    private NodeChunkManage nodeChunkManage;
    private WorkFlowPostHandler workFlowPostHandler;
    private Object currentNode;
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
    private ExecutorService executorService=Executors.newFixedThreadPool(5);

    public WorkflowManage(Flow flow, FlowParams params, WorkFlowPostHandler workFlowPostHandler,
                          BaseToResponse baseToResponse, Map<String, Object> formData, List<Object> imageList,
                          List<Object> documentList, List<Object> audioList, String startNodeId,
                          Object startNodeData, ApplicationChatRecordVO chatRecord, Object childNode) {
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
                        result.set(execIndex,(content.isEmpty() ? currentAnswer.getContent() : content + "\n\n" + currentAnswer.getContent()));
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

    private void loadNode(Object chatRecord, String startNodeId, Object startNodeData) {
        // 实现细节取决于你的具体需求
    }

    public Flux<JSONObject> run() {
     //   closeOldConnections();
      //  String language = getLanguage();
        String language = "zh";
        if (params.getStream()) {
            return runStream(startNode, null, language);
        } else {
            return runBlock(language);
        }
    }

    //todo 改造成Flux<JSONObject>
    public Flux<JSONObject> runStream(INode currentNode, Future<NodeResultFuture> nodeResultFuture, String language) {
        runChainAsync(currentNode, nodeResultFuture, language);
        return awaitResult();
    }

    public Iterator<String> toStreamResponseSimple(Iterator<String> iterator) {
        return iterator;
    }

    public Flux<JSONObject> awaitResult() {
        NodeChunkManage nodeChunkManage = this.nodeChunkManage;
        WorkflowManage workflow = this;
        AtomicBoolean isFinished = new AtomicBoolean(false);
        System.out.println(isFinished);
        return Flux.create(sink -> {
            // 使用单独线程来模拟异步操作
            System.out.println("nodeChunkManage="+nodeChunkManage);
            while (isRun()) {
                System.out.println("isRun");
                JSONObject chunk = nodeChunkManage.pop();
                if (chunk != null) {
                    sink.next(chunk);
                }
            }

            // 处理结束后的工作流
            Map<String, JSONObject> details = getRuntimeDetails();
            int messageTokens = details.values().stream()
                    .filter(row -> row.containsKey("message_tokens") && row.get("message_tokens") != null)
                    .mapToInt(row -> (int) row.get("message_tokens"))
                    .sum();
            int answerTokens = details.values().stream()
                    .filter(row -> row.containsKey("answer_tokens") && row.get("answer_tokens") != null)
                    .mapToInt(row -> (int) row.get("answer_tokens"))
                    .sum();

            workFlowPostHandler.handler(params.getChatId(), params.getChatRecordId(),
                    answer, workflow);

            sink.next(baseToResponse.toStreamChunkResponse(params.getChatId(),
                    params.getChatRecordId(), "", new LinkedList<>(), "", true, messageTokens, answerTokens));
            sink.complete(); // 结束整个 Flux 流

            isFinished.set(true); // 标记完成
        });
    }


    public void runChainAsync(INode currentNode,Future<NodeResultFuture> nodeResultFuture, String language) {
        // 提交任务给线程池执行，并获取对应的Future对象
        Future<?> future = executorService.submit(() -> {
            try {
                runChainManage(currentNode, nodeResultFuture, language);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println(future);
        // 将Future对象添加到futureList列表中
        futureList.add(future);
    }

    public INode getNode(String type) {
        System.out.println("nodeContext="+nodeContext);
        for (INode iNode : nodeContext) {
            if (iNode.getType().equals(type)){
                System.out.println("iNode="+iNode);
                return iNode;
            }
        }
        return null;
     //   return nodeContext.parallelStream().filter(node -> node.getType().equals(type)).findFirst().orElse(null);
    }

    public Node getStartNode() {
        for (Node node : this.flow.getNodes()) {
            if("start-node".equals(node.getType())){
                return node;
            }
        }
        return null;
      //  return this.flow.getNodes().parallelStream().filter(node -> node.getType().equals("start-node")).findFirst().orElse(null);
    }

    public void runChainManage(INode currentNode, Future<NodeResultFuture> nodeResultFuture, String language) throws InterruptedException, ExecutionException {
        // 激活翻译（假设有一个类似功能的类）
       // Translation.activate(language);
        System.out.println("runChainManage");
        if (currentNode == null) {
            Node startNode = getStartNode();
            System.out.println("startNode="+startNode);
            currentNode = NodeFactory.getNode(startNode.getType());
            System.out.println("currentNode="+currentNode);
        }

        // 添加节点块
        nodeChunkManage.addNodeChunk(currentNode.getNodeChunk());

        // 添加节点
        appendNode(currentNode);

        // 执行链式任务
        NodeResult result = runChain(currentNode, nodeResultFuture.get());

        if (result == null) {
            return;
        }

        // 获取下一个节点列表
        List<INode> nodeList = getNextNodeList(currentNode, result);

        if (nodeList.size() == 1) {
            runChainManage(nodeList.get(0), null, language);
        } else if (nodeList.size() > 1) {
            // 对节点进行排序
            List<INode> sortedNodeRunList = nodeList.stream()
                    .sorted(Comparator.comparingInt(n -> n.getNode().getY()))
                    .toList();

            // 提交子任务并获取Future对象
            List<Future<?>> resultList = new ArrayList<>();
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
                resultList.add(future);
            }

            // 等待所有子任务完成（可选）
            for (Future<?> future : resultList) {
                future.get(); // 这里会阻塞直到子任务完成
            }
        }
    }

    private List<INode> getNextNodeList(INode currentNode, NodeResult result) {
        return List.of();
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
        try {
            NodeResult currentResult = nodeResultFuture.getResult();
            Map<String, JSONObject>  result = currentResult.writeContext(currentNode, this);
            if (result != null) {
                if (isResult(currentNode, currentResult)) {
                    for (JSONObject r : result.values()) {
                        Object content = r;
                        child_node = new HashMap<>();
                        boolean node_is_end = false;
                        view_type = currentNode.getViewType();
                        if (r != null) {
                            content = ((Map<String, Object>) r).get("content");
                            child_node.put("runtime_node_id", ((Map<String, Object>) r).get("runtime_node_id"));
                            child_node.put("chat_record_id", ((Map<String, Object>) r).get("chat_record_id"));
                            child_node.put("child_node", ((Map<String, Object>) r).get("child_node"));
                            realNodeId = (String) ((Map<String, Object>) r).get("real_node_id");
                            node_is_end = (boolean) r.getOrDefault("node_is_end", false);
                            view_type = (String) ((Map<String, Object>) r).get("view_type");
                        }
                        JSONObject chunk = this.getBaseToResponse().toStreamChunkResponse(getParams().getChatId(),
                                getParams().getChatRecordId(),
                                currentNode.getId(),
                                currentNode.getUpNodeIdList(),
                                content.toString(), false, 0, 0,
                                new ChunkInfo(currentNode.getType(),
                                        currentNode.runtimeNodeId,
                                        view_type,
                                        child_node,
                                        node_is_end,
                                        realNodeId));
                        currentNode.getNodeChunk().addChunk(chunk);
                    }
                    JSONObject endChunk = this.getBaseToResponse().toStreamChunkResponse(getParams().getChatId(),
                            getParams().getChatRecordId(),
                            currentNode.getId(),
                            currentNode.getUpNodeIdList(),
                            "", false, 0, 0,
                            new ChunkInfo(currentNode.getType(),
                                    realNodeId,
                                    view_type,
                                    child_node,
                                    true,
                                    realNodeId));
                    currentNode.getNodeChunk().addChunk(endChunk);
                } else {
                    // Assuming list(result) is meant to process each item in result
                    for (Object ignored : result.values()) {}
                }
                return currentResult;
            }
        } catch (Exception e) {
            // Exception handling
            //StackTrace.print(e); // Placeholder for exception logging
            JSONObject errorChunk = this.getBaseToResponse().toStreamChunkResponse(getParams().getChatId(),
                    getParams().getChatRecordId(),
                    currentNode.getId(),
                    currentNode.getUpNodeIdList(),
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
            return null;
        } finally {
            currentNode.getNodeChunk().end(null);
        }
        return null;
    }

    public NodeResult handleNodeResult(INode currentNode, NodeResultFuture nodeResultFuture) {
        return null;
    }


    public Flux<JSONObject> runBlock(String language) {
        if (language == null || language.isEmpty()) {
            language = "zh";
        }

        runChainAsync(null, null, language);
        while (!isRun()) {
            break;
        }

        Map<String, JSONObject> details = getRuntimeDetails();
        int messageTokens = details.values().stream()
                .filter(row -> row.containsKey("message_tokens") && row.get("message_tokens") != null)
                .mapToInt(row -> ((Number) row.get("message_tokens")).intValue())
                .sum();

        int answerTokens = details.values().stream()
                .filter(row -> row.containsKey("answer_tokens") && row.get("answer_tokens") != null)
                .mapToInt(row -> ((Number) row.get("answer_tokens")).intValue())
                .sum();

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

        workFlowPostHandler.handler(chatId, chatRecordId, answerText.toString(), this);

        baseToResponse.toBlockResponse(chatId,
                chatRecordId,
                answerText.toString(),
                true,
                messageTokens,
                answerTokens,
                getStatus());
        return Flux.just(new JSONObject());
    }

    public boolean isRun() {
        return isRun(500, TimeUnit.MILLISECONDS);
    }

    public boolean isRun(long timeout, TimeUnit timeUnit) {
        int futureListLen = futureList.size();

        // 创建两个列表用于存储已完成和未完成的任务
        List<Future<?>> done = new ArrayList<>();
        List<Future<?>> notDone = new ArrayList<>(futureList);

        long endTime = System.nanoTime() + timeUnit.toNanos(timeout);
        boolean timeoutReached = false;

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
        return "";
    }

    public Map<String, JSONObject> getRuntimeDetails() {
        Map<String, JSONObject> detailsResult = new HashMap<>();

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
