package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.KnowledgeWorkflow;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.module.chat.dto.DataSource;
import com.tarzan.maxkb4j.module.chat.dto.KnowledgeParams;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.DocumentSimple;
import com.tarzan.maxkb4j.module.knowledge.service.DocumentWebService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@NodeHandlerType(NodeType.DATA_SOURCE_WEB)
@RequiredArgsConstructor
public class DataSourceWebHandler implements INodeHandler {

    private final DocumentWebService documentWebService;

    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        List<DocumentSimple> documentList = new ArrayList<>();
        Map<String, Object> inputParams = new HashMap<>();
        if (workflow instanceof KnowledgeWorkflow knowledgeWorkflow) {
            KnowledgeParams knowledgeParams = knowledgeWorkflow.getKnowledgeParams();
            DataSource dataSource = knowledgeParams.getDataSource();
            if (dataSource != null) {
                String sourceUrl = dataSource.getSourceUrl();
                String selector = dataSource.getSelector() == null ? "body" : dataSource.getSelector();
                inputParams.put("sourceUrl", sourceUrl);
                inputParams.put("selector", selector);
                documentList = documentWebService.getDocumentList(sourceUrl, selector,true);
            }
        }
        node.getDetail().put("inputParams", inputParams);
        node.getDetail().put("outputParams", documentList);
        return new NodeResult(Map.of("documentList", documentList));
    }


}
