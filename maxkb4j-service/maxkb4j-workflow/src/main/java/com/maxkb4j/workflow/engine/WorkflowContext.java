package com.maxkb4j.workflow.engine;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.workflow.model.IWorkflowContext;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 工作流上下文管理器
 * 负责管理工作流的各级上下文：全局上下文、聊天上下文、节点上下文
 */
@Data
public class WorkflowContext implements IWorkflowContext {

    /**
     * 全局变量上下文
     * -- GETTER --
     * 获取或设置全局变量
     */
    private final Map<String, Object> globalContext;
    /**
     * 聊天变量上下文
     * 获取或设置聊天上下文变量
     */
    private final Map<String, Object> chatContext;
    /**
     * 节点变量上下文列表
     * 获取或设置节点上下文
     */
    private final List<AbsNode> nodeContext;

    private Map<String, Object> loopContext;

    protected VariableResolver variableResolver;
    protected TemplateRenderer templateRenderer;


    public WorkflowContext() {
        this.globalContext = new ConcurrentHashMap<>();
        this.chatContext = new ConcurrentHashMap<>();
        this.nodeContext = new CopyOnWriteArrayList<>();
        this.loopContext = new ConcurrentHashMap<>();
        // 2. 依赖组件初始化（顺序敏感）
        this.variableResolver = new VariableResolver(this);
        this.templateRenderer = new TemplateRenderer(this.variableResolver);
    }

    /**
     * 添加节点到上下文
     */
    @Override
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

    @Override
    public String render(String prompt) {
        return templateRenderer.render(prompt);
    }

    @Override
    public String render(String prompt, Map<String, Object> addVariables) {
        return templateRenderer.render(prompt, addVariables);
    }

    @Override
    public Map<String, Object> getPromptVariables() {
        return variableResolver.getPromptVariables();
    }

    @Override
    public Object getReferenceField(List<String> reference) {
        if (CollectionUtils.isNotEmpty(reference) && reference.size() > 1) {
            return getReferenceField(reference.get(0), reference.get(1));
        }
        return null;
    }

    @Override
    public Object getReferenceField(String nodeId, String key) {
        return variableResolver.getReferenceField(nodeId, key);
    }

    @Override
    public Object getFieldValue(Object value, String source) {
        if ("reference".equals(source) && value instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> fields = (List<String>) value;
            if (fields.size() >= 2){
                return getReferenceField(fields.get(0), fields.get(1));
            }
        }
        return value;
    }

    @Override
    public AbsNode getExecutedNode(String nodeId) {
        Optional<AbsNode> optional=nodeContext.stream().filter(node -> node.getId().equals(nodeId)).findFirst();
        return optional.orElse(null);
    }

}
