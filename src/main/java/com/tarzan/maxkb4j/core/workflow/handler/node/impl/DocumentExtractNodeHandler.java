package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.core.workflow.model.SysFile;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.DocumentExtractNode;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.DocumentSimple;
import com.tarzan.maxkb4j.module.knowledge.service.DocumentParseService;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;

@NodeHandlerType(NodeType.DOCUMENT_EXTRACT)
@RequiredArgsConstructor
@Component
public class DocumentExtractNodeHandler implements INodeHandler {

    private final DocumentParseService documentParseService;
    private final MongoFileService fileService;

    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        // 1. 解析节点参数
        DocumentExtractNode.NodeParams nodeParams = node.getNodeData()
                .toJavaObject(DocumentExtractNode.NodeParams.class);

        // 2. 安全校验 documentList
        if (nodeParams == null || nodeParams.getDocumentList() == null || nodeParams.getDocumentList().size() < 2) {
            throw new IllegalArgumentException("Invalid documentList in node params: expected at least two elements");
        }

        // 3. 获取引用字段（workflow 中的文档列表）
        Object res = workflow.getReferenceField(nodeParams.getDocumentList().get(0), nodeParams.getDocumentList().get(1));
        List<SysFile> documentFiles;
        if (res == null) {
            documentFiles = Collections.emptyList();
        } else if (res instanceof List<?>) {
            // 安全转型：只保留 SysFile 类型
            documentFiles = new ArrayList<>();
            for (Object item : (List<?>) res) {
                if (item instanceof SysFile) {
                    documentFiles.add((SysFile) item);
                }
                // 可选：记录非 SysFile 元素（或抛异常）
            }
        } else {
            throw new IllegalArgumentException("Expected List<SysFile> from reference field, but got: " + res.getClass());
        }

        // 4. 并行处理（若文件多且解析耗时，可考虑并行流，但需注意线程安全）
        List<String> contentList = new LinkedList<>();
        List<DocumentSimple> documentList = new ArrayList<>();

        for (SysFile sysFile : documentFiles) {
            InputStream ins = fileService.getStream(sysFile.getFileId());
            String text =  documentParseService.extractText(sysFile.getName(), ins);
            contentList.add(text);
            documentList.add(new DocumentSimple(sysFile.getName(), text,sysFile.getFileId()));
        }

        // 5. 返回结果
        return new NodeResult(Map.of(
                "content", String.join(DocumentExtractNode.SPLITTER, contentList),
                "documentList", documentList
        ));
    }


}
