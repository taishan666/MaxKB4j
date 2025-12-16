package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.core.workflow.model.SysFile;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.node.impl.DocumentExtractNode;
import com.tarzan.maxkb4j.module.knowledge.service.DocumentParseService;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@NodeHandlerType(NodeType.DOCUMENT_EXTRACT)
@RequiredArgsConstructor
@Component
public class DocumentExtractNodeHandler implements INodeHandler {

    private final MongoFileService fileService;
    private final DocumentParseService documentParseService;

    @SuppressWarnings("unchecked")
    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        String splitter = "\n-----------------------------------\n";
        DocumentExtractNode.NodeParams nodeParams=node.getNodeData().toJavaObject(DocumentExtractNode.NodeParams.class);
        List<String> documentList=nodeParams.getDocumentList();
        List<String> content=new LinkedList<>();
        Object res=workflow.getReferenceField(documentList.get(0),documentList.get(1));
        List<SysFile> documents= res==null?List.of():(List<SysFile>) res;
        for (SysFile sysFile : documents) {
            InputStream fileStream= fileService.getStream(sysFile.getFileId());
            String text=documentParseService.extractText(fileStream);
            content.add(text);
        }
        return new NodeResult(Map.of("content",String.join(splitter, content),"documentList",documents));
    }
}
