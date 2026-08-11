package com.maxkb4j.workflow.node;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.Answer;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChildNode;
import com.maxkb4j.common.util.MessageConverter;
import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.util.NodeIdGenerator;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * 节点抽象基类
 * 所有工作流节点的基类，提供通用功能
 * 重构后职责更加清晰：
 * - 使用 NodeIdGenerator 生成运行时ID
 * - 使用 MessageConverter 进行消息转换
 */
@Slf4j
@Data
public abstract class AbsNode {
    private String id;
    private String type;
    private String viewType;
    private JSONObject properties;
    protected Map<String, Object> context;
    protected Map<String, Object> detail;
    private List<String> upNodeIdList;
    private String runtimeNodeId;
    private String answerText;
    private volatile Integer status;
    private String errMessage;

    /** Lock-free CAS updater for status, used to atomically claim node execution. */
    private static final AtomicReferenceFieldUpdater<AbsNode, Integer> STATUS_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(AbsNode.class, Integer.class, "status");

    public AbsNode(String id, JSONObject properties) {
        this.id = id;
        this.properties = properties;
        this.viewType = "many_view";
        this.context = new LinkedHashMap<>(5);
        this.detail = new LinkedHashMap<>(5);
        this.upNodeIdList = new ArrayList<>();
        this.runtimeNodeId = generateRuntimeNodeId();
        this.answerText = "";
        this.status = NodeStatus.READY.getStatus();
        this.errMessage = "";
    }

    /**
     * Atomically claim the right to execute this node by transitioning its status from
     * READY (or INTERRUPT) to STARTED via a CAS. Returns true if this thread won the
     * claim and should proceed to execute; returns false if another thread already
     * claimed or executed it (e.g. diamond-join siblings concurrently reaching the same
     * node), in which case the caller must skip execution to avoid a duplicate run.
     */
    public boolean tryClaimRunning() {
        Integer current = this.status;
        if (NodeStatus.READY.getStatus() == current || NodeStatus.INTERRUPT.getStatus() == current) {
            return STATUS_UPDATER.compareAndSet(this, current, NodeStatus.STARTED.getStatus());
        }
        return false;
    }

    public JSONObject getNodeData() {
        if (Objects.nonNull(properties) && properties.containsKey("nodeData")) {
            return properties.getJSONObject("nodeData");
        }
        return new JSONObject();
    }

    public void setUpNodeIdList(List<String> upNodeIdList) {
        this.upNodeIdList = upNodeIdList;
        this.runtimeNodeId = generateRuntimeNodeId();
    }

    public String getNodeName() {
        return properties.getString("nodeName");
    }

    /**
     * 保存节点上下文
     * <p>
     * 默认空实现，遵循"默认实现原则"——子类无需为不需要持久化上下文的节点强行 override。
     * 需要将上下文持久化到 detail 中的节点（如 AiChat、Question 等）应 override 此方法。
     *
     * @param workflow 工作流实例
     * @param detail   节点详情
     */
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        // 默认空实现，子类按需 override
    }

    /**
     * 生成运行时节点ID
     * 使用 NodeIdGenerator 工具类
     *
     * @return 运行时节点ID
     */
    private String generateRuntimeNodeId() {
        return NodeIdGenerator.generateRuntimeNodeId(id, upNodeIdList);
    }

    public List<Answer> getAnswerList(String chatRecordId) {
        if (StringUtils.isNotBlank(answerText)) {
            Object value = context.get("reasoningContent");
            String reasoningContent = value != null ? String.valueOf(value) : "";
            return List.of(Answer.builder()
                    .content(answerText)
                    .reasoningContent(reasoningContent)
                    .chatRecordId(chatRecordId)
                    .runtimeNodeId(runtimeNodeId)
                    .realNodeId(runtimeNodeId)
                    .viewType(viewType)
                    .build());
        }
        return List.of();
    }

    /**
     * 转换为聊天消息VO
     * 使用 MessageConverter 工具类
     *
     * @param chatId           聊天ID
     * @param chatRecordId     聊天记录ID
     * @param content          消息内容
     * @param reasoningContent 推理内容
     * @param childNode        子节点
     * @param nodeIsEnd        节点是否结束
     * @return 聊天消息VO
     */
    public ChatMessageVO toChatMessageVO(String chatId, String chatRecordId, String content, String reasoningContent, ChildNode childNode, boolean nodeIsEnd) {
        return toChatMessageVO(chatId, chatRecordId, this.getNodeName(), content, reasoningContent, childNode, nodeIsEnd);
    }

    public ChatMessageVO toChatMessageVO(String chatId, String chatRecordId, String nodeName,String content, String reasoningContent, ChildNode childNode, boolean nodeIsEnd) {
        String realNodeId=this.getRuntimeNodeId();
        if (childNode!=null){
            realNodeId=childNode.getRuntimeNodeId();
        }
        if (nodeName==null){
            nodeName=this.getNodeName();
        }
        return MessageConverter.toChatMessageVO(
                chatId,
                chatRecordId,
                this.getId(),
                nodeName,
                content,
                reasoningContent,
                this.getUpNodeIdList(),
                this.getRuntimeNodeId(),
                realNodeId,
                this.getType(),
                this.getViewType(),
                childNode,
                nodeIsEnd,
                false);
    }

}



