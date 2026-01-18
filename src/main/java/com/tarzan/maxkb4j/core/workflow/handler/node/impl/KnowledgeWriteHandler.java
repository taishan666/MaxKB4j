package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.KnowledgeWorkflow;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.KnowledgeWriteNode;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.DocumentSimple;
import com.tarzan.maxkb4j.module.knowledge.enums.KnowledgeType;
import com.tarzan.maxkb4j.module.knowledge.service.DocumentService;
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
    private final DocumentService documentService;

    @SuppressWarnings("unchecked")
    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        KnowledgeWriteNode.NodeParams nodeParams = node.getNodeData().toJavaObject(KnowledgeWriteNode.NodeParams.class);
        Object value = workflow.getReferenceField(nodeParams.getDocumentList().get(0),nodeParams.getDocumentList().get(1));
        node.getDetail().put("write_content", value);
        if (workflow instanceof KnowledgeWorkflow knowledgeWorkflow) {
            boolean debug = knowledgeWorkflow.getKnowledgeParams().isDebug();
            if (!debug){
                String knowledgeId = knowledgeWorkflow.getKnowledgeParams().getKnowledgeId();
                List<DocumentSimple> docs=(List<DocumentSimple>)value;
                documentService.batchCreateDocs(knowledgeId, KnowledgeType.WORKFLOW.getType(), docs);
            }
        }
        return new NodeResult(Map.of());
    }
}
