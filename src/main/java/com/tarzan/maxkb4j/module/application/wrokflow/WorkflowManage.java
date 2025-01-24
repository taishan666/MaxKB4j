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

        for (int index = 0; index < answerList.size(); index++) {
            Answer currentAnswer = answerList.get(index);

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

    public String generatePrompt(String prompt){
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
}
