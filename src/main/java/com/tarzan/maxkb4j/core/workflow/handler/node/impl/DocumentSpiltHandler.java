package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.core.workflow.model.SysFile;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.node.impl.DocumentSpiltNode;
import com.tarzan.maxkb4j.module.knowledge.service.DocumentParseService;
import com.tarzan.maxkb4j.module.knowledge.service.DocumentSpiltService;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@NodeHandlerType(NodeType.DOCUMENT_SPLIT)
@RequiredArgsConstructor
public class DocumentSpiltHandler implements INodeHandler {

    private final MongoFileService fileService;
    private final DocumentParseService documentParseService;
    private final DocumentSpiltService documentSpiltService;

    @SuppressWarnings("unchecked")
    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        DocumentSpiltNode.NodeParams nodeParams=node.getNodeData().toJavaObject(DocumentSpiltNode.NodeParams.class);
        List<String> documentList=nodeParams.getDocumentList();
        Object res=workflow.getReferenceField(documentList.get(0),documentList.get(1));
        List<SysFile> fileList= res==null?List.of():(List<SysFile>) res;
        for (SysFile sysFile : fileList) {
            InputStream inputStream= fileService.getStream(sysFile.getFileId());
            String content=documentParseService.extractText(inputStream);
            List<String> chunks=split(content,nodeParams.getPatterns(),nodeParams.getLimit(),false);
        }
        return new NodeResult(Map.of("paragraphList", List.of()));
    }

    public List<String> split(String content, String[] patterns, Integer limit, Boolean withFilter) throws IOException {
        List<TextSegment> textSegments = Collections.emptyList();
        if (StringUtils.isNotBlank(content)) {
            textSegments = documentSpiltService.split(content, patterns, limit, withFilter);
        }
        return textSegments.stream().map(TextSegment::text).toList();
    }
}
