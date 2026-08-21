package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.common.domain.dto.OssFile;
import com.maxkb4j.knowledge.dto.DocumentSimple;
import com.maxkb4j.knowledge.service.IDocumentParseService;
import com.maxkb4j.oss.service.IOssService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.DocumentExtractNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;
import static com.maxkb4j.workflow.consts.WorkflowConstants.*;

@NodeHandlerType(NodeType.DOCUMENT_EXTRACT)
@RequiredArgsConstructor
@Component
public class DocumentExtractNodeHandler extends AbsNodeHandler {

    private final IDocumentParseService documentParseService;
    private final IOssService ossService;

    @Override
    protected NodeResult doExecute(IWorkflow workflow, AbsNode node) throws Exception {
        DocumentExtractNode.NodeParams params = parseParams(node, DocumentExtractNode.NodeParams.class);
        // 安全校验 documentList
        if (params == null || params.getDocumentList() == null || params.getDocumentList().size() < 2) {
            throw new IllegalArgumentException("Invalid documentList in node params: expected at least two elements");
        }
        List<OssFile> documentFiles = getOssFiles(workflow,params.getDocumentList());
        // 处理文档
        List<String> contentList = new LinkedList<>();
        List<DocumentSimple> documentList = new ArrayList<>();
        for (OssFile sysFile : documentFiles) {
            // GridFS 流占用 Mongo 连接/游标，必须显式关闭
            try (InputStream ins = ossService.getStream(sysFile.getFileId())) {
                String text = documentParseService.extractText(sysFile.getName(), ins);
                contentList.add(text);
                documentList.add(new DocumentSimple(sysFile.getName(), text, sysFile.getFileId()));
            }
        }

        return new NodeResult(Map.of(
                NodeField.CONTENT, String.join(DocumentExtractNode.SPLITTER, contentList),
                NodeField.DOCUMENT_LIST, documentList
        ));
    }
}
