package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.*;
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
import java.util.*;

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
        List<String> fileIds=nodeParams.getDocumentList();
        Object res=workflow.getReferenceField(fileIds.get(0),fileIds.get(1));
        List<SysFile> fileList= res==null?List.of():(List<SysFile>) res;
        List<Document> documentList=new LinkedList<>();
        for (SysFile sysFile : fileList) {
            InputStream inputStream= fileService.getStream(sysFile.getFileId());
            String content=documentParseService.extractText(inputStream);
            Document document=new Document();
            document.setName(sysFile.getName());
            document.setMeta(new JSONObject());
            List<Paragraph>  paragraphs=new ArrayList<>();
            List<String> chunks;
            if ("qa".equals(nodeParams.getSplitStrategy())){
                //todo
                chunks=new ArrayList<>();
            }else {
                 chunks=split(content,nodeParams.getPatterns(),nodeParams.getChunkSize(),nodeParams.getWithFilter());
            }
            for (int i = 0; i < chunks.size(); i++) {
                Paragraph paragraph=new Paragraph();
                paragraph.setContent(chunks.get(i));
                paragraph.setIsActive(i==0);
                paragraphs.add(paragraph);
            }
            document.setParagraphs(paragraphs);
            documentList.add(document);
        }
        if (workflow instanceof KnowledgeWorkflow knowledgeWorkflow) {
            String knowledgeId = knowledgeWorkflow.getKnowledgeParams().getKnowledgeId();
            documentList.forEach(document -> document.setKnowledgeId(knowledgeId));
        }
        node.getDetail().put("splitStrategy",nodeParams.getSplitStrategy());
        node.getDetail().put("chunkSize",nodeParams.getChunkSize());
        node.getDetail().put("documentList", JSON.toJSON(documentList));
        return new NodeResult(Map.of("paragraphList",documentList));
    }

    public List<String> split(String content, String[] patterns, Integer limit, Boolean withFilter) throws IOException {
        List<TextSegment> textSegments = Collections.emptyList();
        if (StringUtils.isNotBlank(content)) {
            textSegments = documentSpiltService.split(content, patterns, limit, withFilter);
        }
        return textSegments.stream().map(TextSegment::text).toList();
    }
}
