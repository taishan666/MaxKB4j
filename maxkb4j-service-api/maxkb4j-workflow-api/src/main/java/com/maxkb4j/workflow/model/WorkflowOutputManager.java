package com.maxkb4j.workflow.model;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.maxkb4j.common.domain.dto.Answer;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.workflow.enums.WorkflowMode;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Getter;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工作流输出管理器
 * 负责管理工作流的输出：响应式输出、答案列表、运行时详情等
 *
 * 从 Workflow 类提取，遵循单一职责原则
 */
@Getter
public class WorkflowOutputManager {

    /**
     * 工作流配置
     */
    private final WorkflowConfiguration configuration;

    /**
     * 工作流上下文
     */
    private final WorkflowContext context;

    /**
     * 响应式输出 Sink
     */
    @JsonIgnore
    private final Sinks.Many<ChatMessageVO> sink;

    public WorkflowOutputManager(WorkflowConfiguration configuration,
                                 WorkflowContext context,
                                 Sinks.Many<ChatMessageVO> sink) {
        this.configuration = configuration;
        this.context = context;
        this.sink = sink;
    }

    /**
     * 判断是否需要 Sink 输出
     * 知识库工作流不需要输出，聊天工作流需要输出
     *
     * @return 是否需要输出
     */
    public boolean needsSinkOutput() {
        WorkflowMode mode = configuration.getWorkflowMode();
        return mode == WorkflowMode.APPLICATION || mode == WorkflowMode.APPLICATION_LOOP;
    }

    /**
     * 获取答案文本列表
     *
     * @return 答案列表
     */
    public List<Answer> getAnswerTextList() {
        List<AbsNode> validNodes = getValidNodes();
        if (validNodes.isEmpty()) {
            return List.of();
        }
        String chatRecordId = configuration.getChatParams() != null
                ? configuration.getChatParams().getChatRecordId()
                : null;
        if (chatRecordId == null) {
            return List.of();
        }
        List<Answer> answerList = new ArrayList<>(validNodes.size());
        for (AbsNode node : validNodes) {
            answerList.addAll(node.getAnswerList(chatRecordId));
        }
        return answerList;
    }

    /**
     * 获取运行时详情
     *
     * @return 节点运行时详情 JSON
     */
    public JSONObject getRuntimeDetails() {
        JSONObject result = new JSONObject(true);
        List<AbsNode> validNodes = getValidNodes();
        if (validNodes.isEmpty()) {
            return result;
        }
        for (int index = 0; index < validNodes.size(); index++) {
            AbsNode node = validNodes.get(index);
            JSONObject runtimeDetail = new JSONObject(true);
            runtimeDetail.putAll(node.getDetail());
            runtimeDetail.put("index", index);
            runtimeDetail.put("nodeId", node.getId());
            runtimeDetail.put("name", node.getProperties() != null
                    ? node.getProperties().getString("nodeName")
                    : node.getType());
            runtimeDetail.put("upNodeIdList", node.getUpNodeIdList());
            runtimeDetail.put("runtimeNodeId", node.getRuntimeNodeId());
            runtimeDetail.put("type", node.getType());
            runtimeDetail.put("status", node.getStatus());
            runtimeDetail.put("errMessage", node.getErrMessage());
            result.put(node.getRuntimeNodeId(), runtimeDetail);
        }
        return result;
    }

    /**
     * 发送消息到 Sink
     *
     * @param message 聊天消息
     */
    public void emitMessage(ChatMessageVO message) {
        if (sink != null && message != null) {
            sink.tryEmitNext(message);
        }
    }

    /**
     * 获取有效节点列表
     * 仅返回在工作流配置中存在的节点
     *
     * @return 有效节点列表
     */
    private List<AbsNode> getValidNodes() {
        Set<String> configuredNodeIds = configuration.getNodes().stream()
                .map(AbsNode::getId)
                .collect(Collectors.toSet());
        return context.getNodeContext().stream()
                .filter(node -> configuredNodeIds.contains(node.getId()))
                .toList();
    }
}