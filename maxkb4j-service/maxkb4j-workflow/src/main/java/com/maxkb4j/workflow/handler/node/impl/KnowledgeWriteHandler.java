package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.INodeHandler;
import com.maxkb4j.workflow.model.KnowledgeWorkflow;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.KnowledgeWriteNode;
import com.maxkb4j.knowledge.consts.KnowledgeType;
import com.maxkb4j.knowledge.dto.DocumentSimple;
import com.maxkb4j.knowledge.service.IDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@NodeHandlerType(NodeType.KNOWLEDGE_WRITE)
@RequiredArgsConstructor
public class KnowledgeWriteHandler implements INodeHandler {
    private final IDocumentService documentService;

    @SuppressWarnings("unchecked")
    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        KnowledgeWriteNode.NodeParams nodeParams = node.getNodeData().toJavaObject(KnowledgeWriteNode.NodeParams.class);
        Object value = workflow.getReferenceField(nodeParams.getDocumentList());
        node.getDetail().put("write_content", value);
        if (workflow instanceof KnowledgeWorkflow knowledgeWorkflow) {
            boolean debug = knowledgeWorkflow.getKnowledgeParams().isDebug();
            if (!debug){
                String knowledgeId = knowledgeWorkflow.getKnowledgeParams().getKnowledgeId();
                List<DocumentSimple> docs=(List<DocumentSimple>)value;
                documentService.batchCreateDocs(knowledgeId, KnowledgeType.WORKFLOW, docs);
            }
        }
        return new NodeResult(Map.of());
    }
}
