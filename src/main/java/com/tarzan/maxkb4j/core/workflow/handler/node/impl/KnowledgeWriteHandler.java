package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.alibaba.fastjson.JSON;
import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.node.impl.KnowledgeWriteNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@NodeHandlerType(NodeType.KNOWLEDGE_WRITE)
@RequiredArgsConstructor
public class KnowledgeWriteHandler implements INodeHandler {
    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        KnowledgeWriteNode.NodeParams nodeParams = node.getNodeData().toJavaObject(KnowledgeWriteNode.NodeParams.class);
        Object value = workflow.getReferenceField(nodeParams.getDocumentList().get(0),nodeParams.getDocumentList().get(1));
        node.getDetail().put("write_content", JSON.toJSON(value));
        return new NodeResult(Map.of());
    }
}
