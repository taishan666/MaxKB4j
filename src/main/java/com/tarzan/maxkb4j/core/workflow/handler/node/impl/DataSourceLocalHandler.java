package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.SysFile;
import com.tarzan.maxkb4j.core.workflow.model.KnowledgeWorkflow;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.node.impl.DataSourceLocalNode;
import com.tarzan.maxkb4j.module.chat.dto.DataSource;
import com.tarzan.maxkb4j.module.chat.dto.KnowledgeParams;
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
public class DataSourceLocalHandler implements INodeHandler {
    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        DataSourceLocalNode.NodeParams nodeParams = node.getNodeData().toJavaObject(DataSourceLocalNode.NodeParams.class);
        if (workflow instanceof KnowledgeWorkflow knowledgeWorkflow) {
            KnowledgeParams knowledgeParams = knowledgeWorkflow.getKnowledgeParams();
            List<SysFile> fileList=new ArrayList<>();
            DataSource dataSource= knowledgeParams.getDataSource();
            if (dataSource != null){
                fileList=dataSource.getFileList();
            }
            Map<String, Object> knowledgeBase= knowledgeParams.getKnowledgeBase();
            if (knowledgeBase != null){
                workflow.getContext().putAll(knowledgeBase);
            }
            return new NodeResult(Map.of("fileList", fileList));
        }
        return new NodeResult(Map.of());
    }
}
