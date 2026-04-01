package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.common.domain.dto.OssFile;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbstractNodeHandler;
import com.maxkb4j.workflow.model.*;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@NodeHandlerType(NodeType.DATA_SOURCE_LOCAL)
@RequiredArgsConstructor
public class DataSourceLocalHandler extends AbstractNodeHandler {


    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node) throws Exception {
        List<OssFile> fileList = new ArrayList<>();
        if (workflow instanceof KnowledgeWorkflow knowledgeWorkflow) {
            KnowledgeParams knowledgeParams = knowledgeWorkflow.getKnowledgeParams();
            DataSource dataSource = knowledgeParams.getDataSource();
            if (dataSource != null) {
                fileList = dataSource.getFileList();
            }
        }
        return new NodeResult(Map.of("fileList", fileList));
    }
}
