package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbstractNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.DocumentExtractNode;
import com.maxkb4j.knowledge.dto.DocumentSimple;
import com.maxkb4j.knowledge.service.IDocumentParseService;
import com.maxkb4j.common.domain.dto.OssFile;
import com.maxkb4j.oss.service.IOssService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;

@NodeHandlerType(NodeType.DOCUMENT_EXTRACT)
@RequiredArgsConstructor
@Component
public class DocumentExtractNodeHandler extends AbstractNodeHandler<DocumentExtractNode.NodeParams> {

    private final IDocumentParseService documentParseService;
    private final IOssService fileService;

    @Override
    protected Class<DocumentExtractNode.NodeParams> getParamsClass() {
        return DocumentExtractNode.NodeParams.class;
    }

    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node, DocumentExtractNode.NodeParams params) throws Exception {
        // 安全校验 documentList
        if (params == null || params.getDocumentList() == null || params.getDocumentList().size() < 2) {
            throw new IllegalArgumentException("Invalid documentList in node params: expected at least two elements");
        }

        // 获取引用字段（workflow 中的文档列表）
        Object res = workflow.getReferenceField(params.getDocumentList());
        List<OssFile> documentFiles;

        if (res == null) {
            documentFiles = Collections.emptyList();
        } else if (res instanceof List<?>) {
            documentFiles = new ArrayList<>();
            for (Object item : (List<?>) res) {
                if (item instanceof OssFile) {
                    documentFiles.add((OssFile) item);
                }
            }
        } else {
            throw new IllegalArgumentException("Expected List<SysFile> from reference field, but got: " + res.getClass());
        }

        // 处理文档
        List<String> contentList = new LinkedList<>();
        List<DocumentSimple> documentList = new ArrayList<>();

        for (OssFile sysFile : documentFiles) {
            InputStream ins = fileService.getStream(sysFile.getFileId());
            String text = documentParseService.extractText(sysFile.getName(), ins);
            contentList.add(text);
            documentList.add(new DocumentSimple(sysFile.getName(), text, sysFile.getFileId()));
        }

        return new NodeResult(Map.of(
                "content", String.join(DocumentExtractNode.SPLITTER, contentList),
                "documentList", documentList
        ));
    }
}
