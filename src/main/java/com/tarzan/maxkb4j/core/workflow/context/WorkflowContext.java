package com.tarzan.maxkb4j.core.workflow.context;

import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 工作流上下文管理器
 * 负责管理工作流的各级上下文：全局上下文、聊天上下文、节点上下文
 */
@Data
public class WorkflowContext {

    /**
     * 全局变量上下文
     * -- GETTER --
     *  获取或设置全局变量

     */
    private final Map<String, Object> globalContext;

    /**
     * 聊天变量上下文
     *  获取或设置聊天上下文变量
     */
    private final Map<String, Object> chatContext;
    /**
     * 节点变量上下文列表
     *  获取或设置节点上下文
     */
    private final List<AbsNode> nodeContext;

    public WorkflowContext() {
        this.globalContext = new HashMap<>();
        this.chatContext = new HashMap<>();
        this.nodeContext = new CopyOnWriteArrayList<>();
    }

    public WorkflowContext(Map<String, Object> globalContext, Map<String, Object> chatContext) {
        this.globalContext = globalContext;
        this.chatContext = chatContext;
        this.nodeContext = new CopyOnWriteArrayList<>();
    }

    /**
     * 添加节点到上下文
     */
    public void appendNode(AbsNode currentNode) {
        for (int i = 0; i < this.nodeContext.size(); i++) {
            AbsNode node = this.nodeContext.get(i);
            if (currentNode.getId().equals(node.getId()) && currentNode.getRuntimeNodeId().equals(node.getRuntimeNodeId())) {
                this.nodeContext.set(i, currentNode);
                return;
            }
        }
        this.nodeContext.add(currentNode);
    }

}
