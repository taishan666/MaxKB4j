package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.knowledge.consts.KnowledgeType;
import com.maxkb4j.knowledge.dto.DocumentSimple;
import com.maxkb4j.knowledge.service.IDocumentService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbstractNodeHandler;
import com.maxkb4j.workflow.model.KnowledgeWorkflow;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.KnowledgeWriteNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@NodeHandlerType(NodeType.KNOWLEDGE_WRITE)
@RequiredArgsConstructor
public class KnowledgeWriteHandler extends AbstractNodeHandler {

    private final IDocumentService documentService;

    @SuppressWarnings("unchecked")
    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node) throws Exception {
        KnowledgeWriteNode.NodeParams params = parseParams(node, KnowledgeWriteNode.NodeParams.class);
        Object value = workflow.getReferenceField(params.getDocumentList());
        putDetail(node, "write_content", value);

        if (workflow instanceof KnowledgeWorkflow knowledgeWorkflow) {
            boolean debug = knowledgeWorkflow.getKnowledgeParams().isDebug();
            if (!debug) {
                String knowledgeId = knowledgeWorkflow.getKnowledgeParams().getKnowledgeId();
                List<DocumentSimple> docs = (List<DocumentSimple>) value;
                documentService.batchCreateDocs(knowledgeId, KnowledgeType.WORKFLOW, docs);
            }
        }

        return new NodeResult(Map.of());
    }
}
