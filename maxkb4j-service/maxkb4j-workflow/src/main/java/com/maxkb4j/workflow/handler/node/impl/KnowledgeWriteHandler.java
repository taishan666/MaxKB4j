package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.knowledge.consts.KnowledgeType;
import com.maxkb4j.knowledge.dto.DocumentSimple;
import com.maxkb4j.knowledge.service.IDocumentService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.model.IKnowledgeWorkflow;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.KnowledgeWriteNode;
import com.maxkb4j.workflow.util.ContextValueUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.maxkb4j.workflow.consts.WorkflowConstants.NodeField;

@Slf4j
@Component
@NodeHandlerType(NodeType.KNOWLEDGE_WRITE)
@RequiredArgsConstructor
public class KnowledgeWriteHandler extends AbsNodeHandler {

    private final IDocumentService documentService;

    @Override
    protected NodeResult doExecute(IWorkflow workflow, AbsNode node) throws Exception {
        KnowledgeWriteNode.NodeParams params = parseParams(node, KnowledgeWriteNode.NodeParams.class);
        Object value = workflow.getReferenceField(params.getDocumentList());
        putDetail(node, NodeField.WRITE_CONTENT, value);

        if (workflow instanceof IKnowledgeWorkflow knowledgeWorkflow) {
            boolean debug = knowledgeWorkflow.getKnowledgeParams().isDebug();
            if (!debug) {
                String knowledgeId = knowledgeWorkflow.getKnowledgeParams().getKnowledgeId();
                // 引用值可能来自恢复的上下文（JSONArray，元素 JSONObject），需安全重建为 DocumentSimple
                List<DocumentSimple> docs = ContextValueUtil.toList(value, DocumentSimple.class);
                documentService.batchCreateDocs(knowledgeId, KnowledgeType.WORKFLOW, docs);
            }
        }

        return new NodeResult(Map.of());
    }
}
