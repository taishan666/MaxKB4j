package com.tarzan.maxkb4j.core.workflow.node;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.enums.NodeStatus;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.service.TemplateRenderer;
import com.tarzan.maxkb4j.core.workflow.util.MessageConverter;
import com.tarzan.maxkb4j.core.workflow.util.NodeIdGenerator;
import com.tarzan.maxkb4j.module.application.domain.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.chat.dto.ChildNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

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
    private Integer status;
    private String errMessage;
    /**
     * 模板渲染器
     */
    private TemplateRenderer templateRenderer;

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

    /**
     * 保存节点上下文
     * 子类需要实现具体逻辑
     *
     * @param workflow 工作流实例
     * @param detail   节点详情
     */
    public abstract void saveContext(Workflow workflow, Map<String, Object> detail);

    /**
     * 生成运行时节点ID
     * 使用 NodeIdGenerator 工具类
     *
     * @return 运行时节点ID
     */
    private String generateRuntimeNodeId() {
        return NodeIdGenerator.generateRuntimeNodeId(id, upNodeIdList);
    }

    public List<String> getAnswerTextList() {
        if (StringUtils.isNotBlank(answerText)){
            return List.of(answerText);
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
        String realNodeId=this.getRuntimeNodeId();
        if (childNode!=null){
            realNodeId=childNode.getRuntimeNodeId();
        }
        return MessageConverter.toChatMessageVO(
                chatId,
                chatRecordId,
                this.getId(),
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



